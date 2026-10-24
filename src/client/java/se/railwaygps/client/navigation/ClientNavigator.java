package se.railwaygps.client.navigation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import se.railwaygps.client.ClientRailwayNetwork;
import se.railwaygps.model.RailwayNetwork;
import se.railwaygps.model.TrackPoint;
import se.railwaygps.navigation.PolylineMatcher;
import se.railwaygps.navigation.RouteLeg;
import se.railwaygps.navigation.RoutePlan;
import se.railwaygps.navigation.RoutePlanner;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class ClientNavigator {
	private static final double TRACK_MATCH_DISTANCE = 6.0;

	private static RoutePlan plan;
	private static int currentLeg;
	private static String networkVersion = "";
	private static NavigationSnapshot snapshot;

	private ClientNavigator() {
	}

	public static StartResult start(LocalPlayer player, String destination) {
		RailwayNetwork network = ClientRailwayNetwork.network();
		if (!network.containsNode(destination)) {
			return new StartResult(false, "Unknown destination '" + destination + "'");
		}
		Optional<String> start = nearestNode(player, network);
		if (start.isEmpty()) {
			return new StartResult(false, "Start navigation near a registered station or junction");
		}
		Optional<RoutePlan> calculated = RoutePlanner.shortestPath(network, start.get(), destination);
		if (calculated.isEmpty()) {
			return new StartResult(false, "No permitted railway route from '" + start.get() + "' to '" + destination + "'");
		}
		plan = calculated.get();
		currentLeg = 0;
		networkVersion = ClientRailwayNetwork.version();
		update(player);
		return new StartResult(true, "Navigating to " + network.nodeName(destination));
	}

	public static void stop() {
		plan = null;
		snapshot = null;
		currentLeg = 0;
	}

	public static Optional<NavigationSnapshot> snapshot() {
		return Optional.ofNullable(snapshot);
	}

	public static void tick(Minecraft client) {
		if (plan == null || client.player == null) {
			return;
		}
		if (!networkVersion.equals(ClientRailwayNetwork.version())) {
			replanAfterNetworkChange(client.player);
		}
		update(client.player);
	}

	private static void update(LocalPlayer player) {
		if (plan == null) {
			return;
		}
		RailwayNetwork network = ClientRailwayNetwork.network();
		if (currentLeg >= plan.legs().size()) {
			snapshot = new NavigationSnapshot(network.nodeName(plan.destination()), plan.destination(), "Destination reached",
					0, 0, 0, NavigationStatus.ARRIVED);
			return;
		}

		RouteLeg leg = plan.legs().get(currentLeg);
		List<TrackPoint> points = leg.reversed() ? leg.route().points().reversed() : leg.route().points();
		Vec3 position = player.position();
		PolylineMatcher.Match match = PolylineMatcher.nearest(points, position.x(), position.y(), position.z()).orElse(null);
		if (match == null) {
			snapshot = uncertain(network, leg);
			return;
		}

		double geometryRemaining = Math.max(0, match.trackLength() - match.distanceAlong());
		double distanceToNext = match.trackLength() == 0
				? leg.route().distance()
				: geometryRemaining * leg.route().distance() / match.trackLength();
		if (match.distanceToTrack() <= TRACK_MATCH_DISTANCE && isNearNode(player, network, leg.to())
				&& geometryRemaining <= network.nodeRadius(leg.to()) + 2) {
			currentLeg++;
			update(player);
			return;
		}

		double remaining = distanceToNext;
		for (int index = currentLeg + 1; index < plan.legs().size(); index++) {
			remaining += plan.legs().get(index).route().distance();
		}
		NavigationStatus status = match.distanceToTrack() <= TRACK_MATCH_DISTANCE
				? NavigationStatus.ON_ROUTE : NavigationStatus.UNCERTAIN;
		String instruction = status == NavigationStatus.ON_ROUTE
				? "Continue toward " + network.nodeName(leg.to()) : "Position uncertain";
		snapshot = new NavigationSnapshot(
				network.nodeName(plan.destination()),
				network.nodeName(leg.to()),
				instruction,
				distanceToNext,
				remaining,
				plan.legs().size() - currentLeg - 1,
				status);
	}

	private static NavigationSnapshot uncertain(RailwayNetwork network, RouteLeg leg) {
		return new NavigationSnapshot(network.nodeName(plan.destination()), network.nodeName(leg.to()), "Position uncertain",
				leg.route().distance(), remainingPlannedDistance(), plan.legs().size() - currentLeg - 1, NavigationStatus.UNCERTAIN);
	}

	private static double remainingPlannedDistance() {
		double distance = 0;
		for (int index = currentLeg; index < plan.legs().size(); index++) {
			distance += plan.legs().get(index).route().distance();
		}
		return distance;
	}

	private static void replanAfterNetworkChange(LocalPlayer player) {
		String destination = plan.destination();
		RailwayNetwork network = ClientRailwayNetwork.network();
		Optional<String> start = nearestNode(player, network);
		Optional<RoutePlan> replacement = start.flatMap(node -> RoutePlanner.shortestPath(network, node, destination));
		if (replacement.isPresent()) {
			plan = replacement.get();
			currentLeg = 0;
		} else {
			stop();
		}
		networkVersion = ClientRailwayNetwork.version();
	}

	private static Optional<String> nearestNode(LocalPlayer player, RailwayNetwork network) {
		String dimension = player.level().dimension().identifier().toString();
		return java.util.stream.Stream.concat(network.stations().keySet().stream(), network.junctions().keySet().stream())
				.filter(id -> dimension.equals(network.nodeDimension(id)))
				.filter(id -> distanceSquared(player.position(), network.nodePosition(id)) <= square(network.nodeRadius(id)))
				.min(Comparator.comparingDouble(id -> distanceSquared(player.position(), network.nodePosition(id))));
	}

	private static boolean isNearNode(LocalPlayer player, RailwayNetwork network, String node) {
		return distanceSquared(player.position(), network.nodePosition(node)) <= square(network.nodeRadius(node));
	}

	private static double distanceSquared(Vec3 position, TrackPoint point) {
		return square(position.x() - (point.x() + 0.5))
				+ square(position.y() - (point.y() + 0.5))
				+ square(position.z() - (point.z() + 0.5));
	}

	private static double square(double value) {
		return value * value;
	}

	public record StartResult(boolean success, String message) {
	}
}
