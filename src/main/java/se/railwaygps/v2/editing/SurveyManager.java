package se.railwaygps.v2.editing;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.level.block.BaseRailBlock;
import se.railwaygps.model.NetworkValidationException;
import se.railwaygps.model.NetworkValidator;
import se.railwaygps.model.TrackGeometry;
import se.railwaygps.model.TrackPoint;
import se.railwaygps.recording.RecordingException;
import se.railwaygps.server.RailwayGpsServer;
import se.railwaygps.v2.model.ControlPoint;
import se.railwaygps.v2.model.RailwayNetworkV2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Records one directed physical rail between two v2 control-point zones. */
public final class SurveyManager {
	private static final double MAX_MOVEMENT_PER_TICK = 8.0;
	private final RailwayGpsServer state;
	private final Map<UUID, Session> sessions = new HashMap<>();

	public SurveyManager(RailwayGpsServer state) {
		this.state = state;
	}

	public void start(ServerPlayer player, String fromId, String toId, String trackId, long tick)
			throws RecordingException {
		if (sessions.containsKey(player.getUUID())) {
			throw new RecordingException("You already have an active v2 survey");
		}
		if (!NetworkValidator.isValidId(trackId)) {
			throw new RecordingException("Invalid track id. Use lowercase letters, numbers, '_' or '-'.");
		}
		RailwayNetworkV2 network = state.networkV2();
		if (network.tracks().containsKey(trackId)) {
			throw new RecordingException("Track '" + trackId + "' already exists");
		}
		ControlPoint from = network.controlPoints().get(fromId);
		ControlPoint to = network.controlPoints().get(toId);
		if (from == null || to == null) {
			throw new RecordingException("Unknown v2 control point");
		}
		if (fromId.equals(toId)) {
			throw new RecordingException("Survey endpoints must be different control points");
		}
		String dimension = dimension(player);
		if (!dimension.equals(from.zone().dimension()) || !dimension.equals(to.zone().dimension())) {
			throw new RecordingException("Both control points must be in your current dimension");
		}
		Entity vehicle = player.getVehicle();
		if (!(vehicle instanceof AbstractMinecart minecart)) {
			throw new RecordingException("You must be riding a minecart to start a survey");
		}
		if (!from.zone().contains(minecart.getX(), minecart.getY(), minecart.getZ())) {
			throw new RecordingException("The minecart is outside the start control point's zone");
		}
		sessions.put(player.getUUID(), new Session(player.getUUID(), minecart, dimension, fromId, toId, trackId, tick));
	}

	public Status status(ServerPlayer player, long tick) throws RecordingException {
		Session session = requireSession(player);
		return new Status(session.trackId, session.fromId, session.toId, session.points.size(),
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
		ControlPoint destination = state.networkV2().controlPoints().get(session.toId);
		if (destination == null || !destination.zone().contains(minecart.getX(), minecart.getY(), minecart.getZ())) {
			throw new RecordingException("The minecart is outside the destination control point's zone");
		}
		session.sample(minecart);
		if (session.jumped) {
			cancelInvalid(player, "Survey cancelled because the minecart position jumped unexpectedly");
			throw new RecordingException("Survey was cancelled without saving");
		}
		List<TrackPoint> simplified = TrackGeometry.simplify(session.points);
		try {
			RailwayNetworkV2 updated = NetworkV2Editor.addRecordedTrack(state.networkV2(), session.trackId,
					session.fromId, session.toId, session.dimension, simplified);
			state.saveV2(updated);
		} catch (IOException | NetworkValidationException | IllegalArgumentException exception) {
			throw new RecordingException("Could not save physical track: " + exception.getMessage(), exception);
		}
		sessions.remove(player.getUUID());
		return new FinishedSurvey(session.trackId, session.points.size(), simplified.size(), TrackGeometry.length(simplified));
	}

	public void cancel(ServerPlayer player) throws RecordingException {
		if (sessions.remove(player.getUUID()) == null) {
			throw new RecordingException("You have no active v2 survey");
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
			session.sample((AbstractMinecart) player.getVehicle());
			if (session.jumped) {
				cancelInvalid(player, "Survey cancelled because the minecart position jumped unexpectedly");
			}
		}
	}

	private Session requireSession(ServerPlayer player) throws RecordingException {
		Session session = sessions.get(player.getUUID());
		if (session == null) {
			throw new RecordingException("You have no active v2 survey");
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

	private static final class Session {
		private final UUID playerId;
		private final UUID minecartId;
		private final String dimension;
		private final String fromId;
		private final String toId;
		private final String trackId;
		private final long startedAtTick;
		private final List<TrackPoint> points = new ArrayList<>();
		private double previousX;
		private double previousY;
		private double previousZ;
		private double distance;
		private boolean jumped;

		private Session(UUID playerId, AbstractMinecart minecart, String dimension, String fromId, String toId,
				String trackId, long startedAtTick) {
			this.playerId = playerId;
			this.minecartId = minecart.getUUID();
			this.dimension = dimension;
			this.fromId = fromId;
			this.toId = toId;
			this.trackId = trackId;
			this.startedAtTick = startedAtTick;
			this.previousX = minecart.getX();
			this.previousY = minecart.getY();
			this.previousZ = minecart.getZ();
			sample(minecart);
		}

		private void sample(AbstractMinecart minecart) {
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
		}
	}

	public record Status(String trackId, String from, String to, int pointCount, double distance, long elapsedTicks) {
	}

	public record FinishedSurvey(String trackId, int rawPointCount, int simplifiedPointCount, double length) {
	}
}
