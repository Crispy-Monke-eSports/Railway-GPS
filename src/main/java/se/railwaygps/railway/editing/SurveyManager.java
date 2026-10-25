package se.railwaygps.railway.editing;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import se.railwaygps.model.NetworkValidationException;
import se.railwaygps.model.TrackGeometry;
import se.railwaygps.model.TrackPoint;
import se.railwaygps.recording.RecordingException;
import se.railwaygps.network.RailwayGpsNetworking;
import se.railwaygps.network.SurveyStatusPayload;
import se.railwaygps.server.RailwayGpsServer;
import se.railwaygps.railway.model.ControlPoint;
import se.railwaygps.railway.model.RailwayNetwork;
import se.railwaygps.railway.model.TraversalRef;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Records a journey and automatically splits it at every visited control-point zone. */
public final class SurveyManager {
	private static final double MAX_MOVEMENT_PER_TICK = 8.0;
	private final RailwayGpsServer state;
	private final Map<UUID, Session> sessions = new HashMap<>();

	public SurveyManager(RailwayGpsServer state) {
		this.state = state;
	}

	public void start(ServerPlayer player, long tick)
			throws RecordingException {
		if (sessions.containsKey(player.getUUID())) {
			throw new RecordingException("You already have an active survey");
		}
		RailwayNetwork network = state.network();
		String dimension = dimension(player);
		Entity vehicle = player.getVehicle();
		if (!(vehicle instanceof AbstractMinecart minecart)) {
			throw new RecordingException("You must be riding a minecart to start a survey");
		}
		BlockPos rail = minecart.getCurrentBlockPosOrRailBelow();
		if (!BaseRailBlock.isRail(minecart.level(), rail)) {
			throw new RecordingException("The minecart must be on real rails to start a survey");
		}
		ControlPoint from = controlPointAt(network, dimension, minecart.getX(), minecart.getY(), minecart.getZ());
		SurveyTrackResolver.Anchor anchor = null;
		if (from == null) {
			try {
				anchor = SurveyTrackResolver.anchor(network, dimension,
						new TrackPoint(rail.getX(), rail.getY(), rail.getZ()));
			} catch (IllegalArgumentException exception) {
				throw new RecordingException(exception.getMessage(), exception);
			}
		}
		Session session = new Session(player.getUUID(), minecart, dimension,
				from == null ? null : from.id(), anchor, tick, network);
		sessions.put(player.getUUID(), session);
		publish(player, session, tick, "recording", "railwaygps.survey.status.recording", "");
	}

	public Status status(ServerPlayer player, long tick) throws RecordingException {
		Session session = requireSession(player);
		String origin = session.startControlPointId != null ? session.startControlPointId
				: "track:" + session.anchor.trackId();
		String latest = session.visits.isEmpty() ? origin : session.visits.getLast().controlPointId;
		return new Status(origin, latest,
				session.visits.size(), session.samples.points().size(),
				session.distance, tick - session.startedAtTick);
	}

	public FinishedSurvey finish(ServerPlayer player) throws RecordingException {
		Session session = requireSession(player);
		AbstractMinecart minecart;
		try {
			minecart = currentMinecart(player, session);
		} catch (RecordingException exception) {
			sessions.remove(player.getUUID());
			throw exception;
		}
		session.sample(minecart, state.network());
		if (session.samples.failure() != null) {
			cancelInvalid(player, failureMessage(session.samples.failure()));
			throw new RecordingException("Survey was cancelled without saving");
		}
		if (state.network().revision() != session.startedRevision) {
			publish(player, session, player.level().getServer().getTickCount(), "recording",
					"railwaygps.survey.status.network_changed", "");
			throw new RecordingException("The railway network changed during the survey; cancel and record again");
		}
		ControlPoint destination = controlPointAt(state.network(), session.dimension,
				minecart.getX(), minecart.getY(), minecart.getZ());
		int minimumVisits = session.anchor == null ? 2 : 1;
		if (destination == null || session.visits.size() < minimumVisits
				|| !destination.id().equals(session.visits.getLast().controlPointId)) {
			publish(player, session, player.level().getServer().getTickCount(), "recording",
					"railwaygps.survey.status.finish_zone", "");
			throw new RecordingException("Finish inside a different control-point zone");
		}
		try {
			RailwayNetwork updated = state.network();
			List<String> trackIds = new ArrayList<>();
			List<TraversalRef> traversals = new ArrayList<>();
			double length = 0;
			int simplifiedPoints = 0;
			if (session.anchor != null) {
				Visit firstVisit = session.visits.getFirst();
				List<TrackPoint> prefix = session.samples.points().subList(0,
						Math.min(session.samples.points().size(), firstVisit.pointIndex + 1));
				TraversalRef anchored = SurveyTrackResolver.resolveAnchorTraversal(updated, session.anchor,
						firstVisit.controlPointId, prefix);
				traversals.add(anchored);
				trackIds.add(anchored.trackId());
			}
			for (int index = 1; index < session.visits.size(); index++) {
				Visit from = session.visits.get(index - 1);
				Visit to = session.visits.get(index);
				if (from.controlPointId.equals(to.controlPointId)) continue;
				List<TrackPoint> points = session.samples.points();
				List<TrackPoint> raw = new ArrayList<>(points.subList(from.pointIndex,
						Math.min(points.size(), to.pointIndex + 1)));
				List<TrackPoint> simplified = TrackGeometry.simplify(raw);
				SurveyTrackResolver.ResolvedTraversal resolved = SurveyTrackResolver.resolveOrCreate(updated,
						from.controlPointId, to.controlPointId, session.dimension, simplified);
				updated = resolved.network();
				traversals.add(resolved.traversal());
				trackIds.add(resolved.traversal().trackId());
				length += TrackGeometry.length(simplified);
				simplifiedPoints += simplified.size();
			}
			for (int index = 1; index < traversals.size(); index++) {
				int visitIndex = session.anchor == null ? index : index - 1;
				updated = addObservedTransition(updated, session.visits.get(visitIndex).controlPointId,
						traversals.get(index - 1), traversals.get(index));
			}
			if (traversals.isEmpty() || updated == state.network()) {
				throw new IllegalArgumentException("No new track direction or transition was recorded");
			}
			state.save(updated);
			sessions.remove(player.getUUID());
			publish(player, session, player.level().getServer().getTickCount(), "finished",
					"railwaygps.survey.status.finished", "");
			return new FinishedSurvey(List.copyOf(trackIds), session.samples.points().size(), simplifiedPoints, length,
					List.copyOf(session.visits.stream().map(Visit::controlPointId).toList()));
		} catch (IOException | NetworkValidationException | IllegalArgumentException exception) {
			publish(player, session, player.level().getServer().getTickCount(), "recording",
					"railwaygps.survey.status.save_failed", exception.getMessage());
			throw new RecordingException("Could not save physical track: " + exception.getMessage(), exception);
		}
	}

	private static RailwayNetwork addObservedTransition(RailwayNetwork network, String controlPointId,
			TraversalRef incoming, TraversalRef outgoing) throws NetworkValidationException {
		boolean exists = network.transitions().values().stream()
				.anyMatch(transition -> transition.enabled()
						&& transition.controlPointId().equals(controlPointId)
						&& transition.incoming().equals(incoming) && transition.outgoing().equals(outgoing));
		if (exists) return network;
		String transitionId = "transition-" + UUID.randomUUID().toString().replace("-", "");
		return RailwayNetworkEditor.addTransition(network, transitionId, controlPointId, incoming, outgoing, null);
	}

	public void cancel(ServerPlayer player) throws RecordingException {
		if (sessions.remove(player.getUUID()) == null) {
			throw new RecordingException("You have no active survey");
		}
		RailwayGpsNetworking.sendSurveyStatus(player, new SurveyStatusPayload("idle",
				"railwaygps.survey.status.cancelled", "", 0, 0, 0, 0));
	}

	public void tick(MinecraftServer server) {
		for (Session session : new ArrayList<>(sessions.values())) {
			ServerPlayer player = server.getPlayerList().getPlayer(session.playerId);
			if (player == null) {
				sessions.remove(session.playerId);
				continue;
			}
			String invalid = invalidReason(player, session);
			if (invalid != null) {
				cancelInvalid(player, invalid);
				continue;
			}
			session.sample((AbstractMinecart) player.getVehicle(), state.network());
			if (session.samples.failure() != null) {
				cancelInvalid(player, failureMessage(session.samples.failure()));
			} else if ((server.getTickCount() - session.startedAtTick) % 10 == 0) {
				publish(player, session, server.getTickCount(), "recording",
						"railwaygps.survey.status.recording", "");
			}
		}
	}

	private static String failureMessage(SurveySampleValidator.Failure failure) {
		return switch (failure.reason()) {
			case POSITION_JUMP -> "Survey cancelled because the minecart position jumped unexpectedly";
			case RAIL_LOST -> "Survey cancelled because the minecart was no longer on a rail";
			case DISCONNECTED_RAILS -> "Survey cancelled because the rail path is discontinuous between "
					+ format(failure.previous()) + " and " + format(failure.current());
		};
	}

	private static String format(TrackPoint point) {
		return point == null ? "an unknown position" : point.x() + ", " + point.y() + ", " + point.z();
	}

	private Session requireSession(ServerPlayer player) throws RecordingException {
		Session session = sessions.get(player.getUUID());
		if (session == null) {
			throw new RecordingException("You have no active survey");
		}
		return session;
	}

	private static AbstractMinecart currentMinecart(ServerPlayer player, Session session) throws RecordingException {
		Entity vehicle = player.getVehicle();
		if (!(vehicle instanceof AbstractMinecart minecart) || !minecart.getUUID().equals(session.minecartId)) {
			throw new RecordingException("Survey was cancelled because you left the original minecart");
		}
		return minecart;
	}

	private static String invalidReason(ServerPlayer player, Session session) {
		if (!player.isAlive()) return "Survey cancelled because you died";
		if (!dimension(player).equals(session.dimension)) return "Survey cancelled because you changed dimension";
		Entity vehicle = player.getVehicle();
		if (!(vehicle instanceof AbstractMinecart minecart)) return "Survey cancelled because you left the minecart";
		if (!minecart.getUUID().equals(session.minecartId)) return "Survey cancelled because you changed minecart";
		if (!minecart.isAlive() || minecart.isRemoved()) return "Survey cancelled because the minecart was destroyed";
		return null;
	}

	private void cancelInvalid(ServerPlayer player, String message) {
		Session session = sessions.remove(player.getUUID());
		String key = "railwaygps.survey.status.invalid";
		String detail = message;
		if (session != null && session.samples.failure() != null) {
			SurveySampleValidator.Failure failure = session.samples.failure();
			key = switch (failure.reason()) {
				case POSITION_JUMP -> "railwaygps.survey.status.jump";
				case RAIL_LOST -> "railwaygps.survey.status.rail_lost";
				case DISCONNECTED_RAILS -> "railwaygps.survey.status.disconnected";
			};
			detail = failure.reason() == SurveySampleValidator.Reason.DISCONNECTED_RAILS
					? format(failure.previous()) + " → " + format(failure.current()) : "";
		}
		RailwayGpsNetworking.sendSurveyStatus(player, new SurveyStatusPayload("error", key, detail,
				session == null ? 0 : session.samples.points().size(), session == null ? 0 : session.visits.size(),
				session == null ? 0 : session.distance, 0));
		player.sendSystemMessage(Component.literal(message + "; no physical track was saved"));
	}

	private static void publish(ServerPlayer player, Session session, long tick, String status,
			String messageKey, String detail) {
		RailwayGpsNetworking.sendSurveyStatus(player, new SurveyStatusPayload(status, messageKey, detail,
				session.samples.points().size(), session.visits.size(), session.distance,
				Math.max(0, tick - session.startedAtTick)));
	}

	private static String dimension(ServerPlayer player) {
		return player.level().dimension().identifier().toString();
	}

	private static ControlPoint controlPointAt(RailwayNetwork network, String dimension, double x, double y, double z) {
		return network.controlPoints().values().stream()
				.filter(point -> dimension.equals(point.zone().dimension()))
				.filter(point -> point.zone().contains(x, y, z))
				.findFirst().orElse(null);
	}

	private static final class Session {
		private final UUID playerId;
		private final UUID minecartId;
		private final String dimension;
		private final String startControlPointId;
		private final SurveyTrackResolver.Anchor anchor;
		private final long startedAtTick;
		private final long startedRevision;
		private final SurveySampleValidator samples = new SurveySampleValidator();
		private final List<Visit> visits = new ArrayList<>();
		private double previousX;
		private double previousY;
		private double previousZ;
		private double distance;

		private Session(UUID playerId, AbstractMinecart minecart, String dimension, String fromId,
				SurveyTrackResolver.Anchor anchor, long startedAtTick, RailwayNetwork network) {
			this.playerId = playerId;
			this.minecartId = minecart.getUUID();
			this.dimension = dimension;
			this.startControlPointId = fromId;
			this.anchor = anchor;
			this.startedAtTick = startedAtTick;
			this.startedRevision = network.revision();
			this.previousX = minecart.getX();
			this.previousY = minecart.getY();
			this.previousZ = minecart.getZ();
			sample(minecart, network);
			if (visits.isEmpty() && fromId != null) visits.add(new Visit(fromId, 0));
		}

		private void sample(AbstractMinecart minecart, RailwayNetwork network) {
			double dx = minecart.getX() - previousX;
			double dy = minecart.getY() - previousY;
			double dz = minecart.getZ() - previousZ;
			double movement = Math.sqrt(dx * dx + dy * dy + dz * dz);
			if (movement <= MAX_MOVEMENT_PER_TICK) distance += movement;
			else samples.observeTeleport();
			previousX = minecart.getX();
			previousY = minecart.getY();
			previousZ = minecart.getZ();

			BlockPos rail = minecart.getCurrentBlockPosOrRailBelow();
			if (!BaseRailBlock.isRail(minecart.level(), rail)) {
				samples.observeMissingRail();
				return;
			}
			BlockState railState = minecart.level().getBlockState(rail);
			BaseRailBlock railBlock = (BaseRailBlock) railState.getBlock();
			RailShape shape = railState.getValue(railBlock.getShapeProperty());
			TrackPoint point = new TrackPoint(rail.getX(), rail.getY(), rail.getZ());
			samples.observeRail(point, shape);
			if (samples.failure() != null) return;
			ControlPoint current = controlPointAt(network, dimension, minecart.getX(), minecart.getY(), minecart.getZ());
			if (current != null && !current.id().equals(visits.isEmpty() ? null : visits.getLast().controlPointId)) {
				visits.add(new Visit(current.id(), Math.max(0, samples.points().size() - 1)));
			}
		}
	}

	private record Visit(String controlPointId, int pointIndex) { }

	public record Status(String from, String latest, int visitedControlPoints, int pointCount,
			double distance, long elapsedTicks) {
	}

	public record FinishedSurvey(List<String> trackIds, int rawPointCount, int simplifiedPointCount,
			double length, List<String> controlPoints) {
	}
}
