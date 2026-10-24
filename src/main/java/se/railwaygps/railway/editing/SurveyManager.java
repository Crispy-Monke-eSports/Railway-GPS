package se.railwaygps.railway.editing;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.level.block.BaseRailBlock;
import se.railwaygps.model.NetworkValidationException;
import se.railwaygps.model.TrackGeometry;
import se.railwaygps.model.TrackPoint;
import se.railwaygps.recording.RecordingException;
import se.railwaygps.server.RailwayGpsServer;
import se.railwaygps.railway.model.ControlPoint;
import se.railwaygps.railway.model.RailwayNetwork;

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
		ControlPoint from = controlPointAt(network, dimension, minecart.getX(), minecart.getY(), minecart.getZ());
		if (from == null) throw new RecordingException("The minecart must be inside a control-point zone");
		sessions.put(player.getUUID(), new Session(player.getUUID(), minecart, dimension, from.id(), tick, network));
	}

	public Status status(ServerPlayer player, long tick) throws RecordingException {
		Session session = requireSession(player);
		return new Status(session.visits.getFirst().controlPointId, session.visits.getLast().controlPointId,
				session.visits.size(), session.points.size(),
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
		if (session.jumped) {
			cancelInvalid(player, "Survey cancelled because the minecart position jumped unexpectedly");
			throw new RecordingException("Survey was cancelled without saving");
		}
		ControlPoint destination = controlPointAt(state.network(), session.dimension,
				minecart.getX(), minecart.getY(), minecart.getZ());
		if (destination == null || session.visits.size() < 2
				|| !destination.id().equals(session.visits.getLast().controlPointId)) {
			throw new RecordingException("Finish inside a different control-point zone");
		}
		try {
			RailwayNetwork updated = state.network();
			List<String> trackIds = new ArrayList<>();
			double length = 0;
			int simplifiedPoints = 0;
			for (int index = 1; index < session.visits.size(); index++) {
				Visit from = session.visits.get(index - 1);
				Visit to = session.visits.get(index);
				if (from.controlPointId.equals(to.controlPointId)) continue;
				List<TrackPoint> raw = new ArrayList<>(session.points.subList(from.pointIndex,
						Math.min(session.points.size(), to.pointIndex + 1)));
				List<TrackPoint> simplified = TrackGeometry.simplify(raw);
				RailwayNetworkEditor.AddedTrack added = RailwayNetworkEditor.addRecordedTrack(updated,
						from.controlPointId, to.controlPointId, session.dimension, simplified);
				updated = added.network();
				trackIds.add(added.trackId());
				length += TrackGeometry.length(simplified);
				simplifiedPoints += simplified.size();
			}
			for (int index = 1; index < trackIds.size(); index++) {
				String transitionId = "transition-" + UUID.randomUUID().toString().replace("-", "");
				updated = RailwayNetworkEditor.addTransition(updated, transitionId,
						session.visits.get(index).controlPointId,
						new se.railwaygps.railway.model.TraversalRef(trackIds.get(index - 1),
								se.railwaygps.railway.model.TrackDirection.FORWARD),
						new se.railwaygps.railway.model.TraversalRef(trackIds.get(index),
								se.railwaygps.railway.model.TrackDirection.FORWARD), null);
			}
			if (trackIds.isEmpty()) throw new IllegalArgumentException("No complete track section was recorded");
			state.save(updated);
			sessions.remove(player.getUUID());
			return new FinishedSurvey(List.copyOf(trackIds), session.points.size(), simplifiedPoints, length,
					List.copyOf(session.visits.stream().map(Visit::controlPointId).toList()));
		} catch (IOException | NetworkValidationException | IllegalArgumentException exception) {
			throw new RecordingException("Could not save physical track: " + exception.getMessage(), exception);
		}
	}

	public void cancel(ServerPlayer player) throws RecordingException {
		if (sessions.remove(player.getUUID()) == null) {
			throw new RecordingException("You have no active survey");
		}
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
			if (session.jumped) {
				cancelInvalid(player, "Survey cancelled because the minecart position jumped unexpectedly");
			}
		}
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
		sessions.remove(player.getUUID());
		player.sendSystemMessage(Component.literal(message + "; no physical track was saved"));
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
		private final long startedAtTick;
		private final List<TrackPoint> points = new ArrayList<>();
		private final List<Visit> visits = new ArrayList<>();
		private double previousX;
		private double previousY;
		private double previousZ;
		private double distance;
		private boolean jumped;

		private Session(UUID playerId, AbstractMinecart minecart, String dimension, String fromId,
				long startedAtTick, RailwayNetwork network) {
			this.playerId = playerId;
			this.minecartId = minecart.getUUID();
			this.dimension = dimension;
			this.startedAtTick = startedAtTick;
			this.previousX = minecart.getX();
			this.previousY = minecart.getY();
			this.previousZ = minecart.getZ();
			sample(minecart, network);
			if (visits.isEmpty()) visits.add(new Visit(fromId, 0));
		}

		private void sample(AbstractMinecart minecart, RailwayNetwork network) {
			double dx = minecart.getX() - previousX;
			double dy = minecart.getY() - previousY;
			double dz = minecart.getZ() - previousZ;
			double movement = Math.sqrt(dx * dx + dy * dy + dz * dz);
			if (movement <= MAX_MOVEMENT_PER_TICK) distance += movement;
			else jumped = true;
			previousX = minecart.getX();
			previousY = minecart.getY();
			previousZ = minecart.getZ();

			BlockPos rail = minecart.getCurrentBlockPosOrRailBelow();
			if (!BaseRailBlock.isRail(minecart.level(), rail)) return;
			TrackPoint point = new TrackPoint(rail.getX(), rail.getY(), rail.getZ());
			if (points.isEmpty() || !points.getLast().equals(point)) points.add(point);
			ControlPoint current = controlPointAt(network, dimension, minecart.getX(), minecart.getY(), minecart.getZ());
			if (current != null && !current.id().equals(visits.isEmpty() ? null : visits.getLast().controlPointId)) {
				visits.add(new Visit(current.id(), Math.max(0, points.size() - 1)));
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
