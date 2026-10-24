package se.railwaygps.recording;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import se.railwaygps.model.Junction;
import se.railwaygps.model.NetworkValidationException;
import se.railwaygps.model.NetworkValidator;
import se.railwaygps.model.RailwayNetwork;
import se.railwaygps.model.Route;
import se.railwaygps.model.TrackGeometry;
import se.railwaygps.model.TrackPoint;
import se.railwaygps.server.RailwayGpsServer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class RecordingManager {
	private final RailwayGpsServer state;
	private final Map<UUID, RecordingSession> sessions = new HashMap<>();

	public RecordingManager(RailwayGpsServer state) {
		this.state = state;
	}

	public void start(ServerPlayer player, String from, String to, String routeId, long tick) throws RecordingException {
		RailwayNetwork network = state.network();
		if (sessions.containsKey(player.getUUID())) {
			throw new RecordingException("You already have an active recording");
		}
		if (!NetworkValidator.isValidId(routeId)) {
			throw new RecordingException("Invalid route id. Use lowercase letters, numbers, '_' or '-'.");
		}
		if (network.routes().containsKey(routeId)) {
			throw new RecordingException("Route '" + routeId + "' already exists");
		}
		if (!network.containsNode(from)) {
			throw new RecordingException("Unknown start node '" + from + "'");
		}
		if (!network.containsNode(to)) {
			throw new RecordingException("Unknown destination node '" + to + "'");
		}
		if (from.equals(to)) {
			throw new RecordingException("Start and destination must be different nodes");
		}

		String dimension = player.level().dimension().identifier().toString();
		if (!dimension.equals(network.nodeDimension(from)) || !dimension.equals(network.nodeDimension(to))) {
			throw new RecordingException("Both route nodes must be in your current dimension");
		}
		Entity vehicle = player.getVehicle();
		if (!(vehicle instanceof AbstractMinecart minecart)) {
			throw new RecordingException("You must be riding a minecart to start recording");
		}
		if (!isNear(minecart, network.nodePosition(from), network.nodeRadius(from))) {
			throw new RecordingException("The minecart is not within the start node's radius");
		}

		sessions.put(player.getUUID(), new RecordingSession(
				player.getUUID(), minecart, dimension, from, to, routeId, tick));
	}

	public RecordingStatus status(ServerPlayer player, long tick) throws RecordingException {
		RecordingSession session = requireSession(player);
		return session.status(tick);
	}

	public void cancel(ServerPlayer player) throws RecordingException {
		if (sessions.remove(player.getUUID()) == null) {
			throw new RecordingException("You have no active recording");
		}
	}

	public FinishedRecording finish(ServerPlayer player) throws RecordingException {
		RecordingSession session = requireSession(player);
		Entity vehicle = player.getVehicle();
		if (!(vehicle instanceof AbstractMinecart minecart) || !minecart.getUUID().equals(session.minecartId())) {
			cancelInvalid(player, "Recording cancelled because you left the original minecart");
			throw new RecordingException("Recording was cancelled without saving");
		}

		RailwayNetwork network = state.network();
		if (!isNear(minecart, network.nodePosition(session.to()), network.nodeRadius(session.to()))) {
			throw new RecordingException("The minecart is not within the destination node's radius");
		}
		session.sample(minecart);
		if (session.hasPositionJump()) {
			cancelInvalid(player, "Recording cancelled because the minecart position jumped unexpectedly");
			throw new RecordingException("Recording was cancelled without saving");
		}
		List<TrackPoint> points = TrackGeometry.simplify(session.points());
		if (points.size() < 2) {
			throw new RecordingException("Not enough rail geometry was recorded; continue driving before finishing");
		}

		double distance = Math.max(session.distance(), TrackGeometry.length(points));
		Route route = new Route(session.routeId(), toDisplayName(session.routeId()), session.from(), session.to(),
				session.dimension(), false, distance, points);
		List<String> warnings = findIntermediateJunctions(network, session, points);
		try {
			state.storage().createRoute(route);
			state.reload();
		} catch (IOException | NetworkValidationException | IllegalArgumentException exception) {
			throw new RecordingException("Could not save route: " + exception.getMessage(), exception);
		}
		sessions.remove(player.getUUID());
		return new FinishedRecording(route, session.points().size(), warnings);
	}

	public void tick(MinecraftServer server) {
		for (RecordingSession session : new ArrayList<>(sessions.values())) {
			ServerPlayer player = server.getPlayerList().getPlayer(session.playerId());
			if (player == null) {
				sessions.remove(session.playerId());
				continue;
			}
			String reason = invalidReason(player, session);
			if (reason != null) {
				cancelInvalid(player, reason);
				continue;
			}
			AbstractMinecart minecart = (AbstractMinecart) player.getVehicle();
			session.sample(minecart);
			if (session.hasPositionJump()) {
				cancelInvalid(player, "Recording cancelled because the minecart position jumped unexpectedly");
			}
		}
	}

	private RecordingSession requireSession(ServerPlayer player) throws RecordingException {
		RecordingSession session = sessions.get(player.getUUID());
		if (session == null) {
			throw new RecordingException("You have no active recording");
		}
		return session;
	}

	private String invalidReason(ServerPlayer player, RecordingSession session) {
		if (!player.isAlive()) {
			return "Recording cancelled because you died";
		}
		if (!player.level().dimension().identifier().toString().equals(session.dimension())) {
			return "Recording cancelled because you changed dimension";
		}
		Entity vehicle = player.getVehicle();
		if (!(vehicle instanceof AbstractMinecart minecart)) {
			return "Recording cancelled because you left the minecart";
		}
		if (!minecart.getUUID().equals(session.minecartId())) {
			return "Recording cancelled because you changed minecart";
		}
		if (!minecart.isAlive() || minecart.isRemoved()) {
			return "Recording cancelled because the minecart was destroyed";
		}
		return null;
	}

	private void cancelInvalid(ServerPlayer player, String message) {
		sessions.remove(player.getUUID());
		player.sendSystemMessage(Component.literal(message + "; no route was saved"));
	}

	private static boolean isNear(AbstractMinecart minecart, TrackPoint point, double radius) {
		if (point == null) {
			return false;
		}
		double dx = minecart.getX() - (point.x() + 0.5);
		double dy = minecart.getY() - point.y();
		double dz = minecart.getZ() - (point.z() + 0.5);
		return dx * dx + dy * dy + dz * dz <= radius * radius;
	}

	private static List<String> findIntermediateJunctions(
			RailwayNetwork network, RecordingSession session, List<TrackPoint> points) {
		List<String> warnings = new ArrayList<>();
		for (Junction junction : network.junctions().values()) {
			if (junction.id().equals(session.from()) || junction.id().equals(session.to())
					|| !junction.dimension().equals(session.dimension())) {
				continue;
			}
			double radiusSquared = junction.radius() * junction.radius();
			boolean passed = points.stream().anyMatch(point -> squaredDistance(point, junction.position()) <= radiusSquared);
			if (passed) {
				warnings.add("Recorded path passes intermediate junction '" + junction.id() + "'");
			}
		}
		return List.copyOf(warnings);
	}

	private static double squaredDistance(TrackPoint first, TrackPoint second) {
		double dx = first.x() - second.x();
		double dy = first.y() - second.y();
		double dz = first.z() - second.z();
		return dx * dx + dy * dy + dz * dz;
	}

	private static String toDisplayName(String id) {
		String normalized = id.replace('_', ' ').replace('-', ' ').trim();
		return normalized.substring(0, 1).toUpperCase(Locale.ROOT) + normalized.substring(1);
	}

	public record FinishedRecording(Route route, int rawPointCount, List<String> warnings) {
	}
}
