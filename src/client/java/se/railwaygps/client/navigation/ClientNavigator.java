package se.railwaygps.client.navigation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import se.railwaygps.client.ClientRailwayNetwork;
import se.railwaygps.model.TrackPoint;
import se.railwaygps.navigation.PolylineMatcher;
import se.railwaygps.navigation.PhysicalInstructionResolver;
import se.railwaygps.navigation.PhysicalRouteLeg;
import se.railwaygps.navigation.PhysicalRoutePlan;
import se.railwaygps.navigation.PhysicalRoutePlanner;
import se.railwaygps.railway.model.ControlPoint;
import se.railwaygps.railway.model.RailwayNetwork;
import se.railwaygps.railway.model.ZoneBox;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class ClientNavigator {
	private static final double TRACK_MATCH_DISTANCE = 6.0;
	private static PhysicalRoutePlan plan;
	private static int currentLeg;
	private static String networkVersion = "";
	private static NavigationSnapshot snapshot;
	private static long revision;

	private ClientNavigator() {
	}

	public static StartResult start(LocalPlayer player, String destination) {
		RailwayNetwork network = ClientRailwayNetwork.network();
		if (!network.controlPoints().containsKey(destination)) {
			return new StartResult(false, Component.translatable("railwaygps.nav.start.unknown_destination", destination));
		}
		Optional<String> start = nearestNode(player, network);
		if (start.isEmpty()) {
			return new StartResult(false, Component.translatable("railwaygps.nav.start.not_near_node"));
		}
		Optional<PhysicalRoutePlan> calculated = PhysicalRoutePlanner.shortestPath(network, start.get(), destination);
		if (calculated.isEmpty()) {
			return new StartResult(false, Component.translatable("railwaygps.nav.start.no_route",
					name(network, start.get()), name(network, destination)));
		}
		plan = calculated.get();
		currentLeg = 0;
		networkVersion = ClientRailwayNetwork.version();
		revision++;
		update(player);
		return new StartResult(true, Component.translatable("railwaygps.nav.start.started", name(network, destination)));
	}

	public static void stop() {
		boolean wasActive = plan != null;
		plan = null;
		snapshot = null;
		currentLeg = 0;
		if (wasActive) {
			revision++;
		}
	}

	public static Optional<NavigationSnapshot> snapshot() {
		return Optional.ofNullable(snapshot);
	}

	public static Optional<PhysicalRoutePlan> activePlan() {
		return Optional.ofNullable(plan);
	}

	public static int currentLegIndex() {
		return currentLeg;
	}

	public static long revision() {
		return revision;
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
			snapshot = new NavigationSnapshot(name(network, plan.destination()), name(network, plan.destination()),
					Component.translatable("railwaygps.instruction.arrived"),
					0, 0, 0, NavigationStatus.ARRIVED);
			return;
		}

		PhysicalRouteLeg leg = plan.legs().get(currentLeg);
		List<TrackPoint> points = leg.points();
		Vec3 position = player.position();
		PolylineMatcher.Match match = PolylineMatcher.nearest(points, position.x(), position.y(), position.z()).orElse(null);
		if (match == null) {
			snapshot = uncertain(network, leg);
			return;
		}

		double geometryRemaining = Math.max(0, match.trackLength() - match.distanceAlong());
		double distanceToNext = match.trackLength() == 0
				? leg.track().length()
				: geometryRemaining * leg.track().length() / match.trackLength();
		if (match.distanceToTrack() <= TRACK_MATCH_DISTANCE && isInsideControlPoint(player, network, leg.to())
				&& geometryRemaining <= 7) {
			currentLeg++;
			revision++;
			update(player);
			return;
		}

		double remaining = distanceToNext;
		for (int index = currentLeg + 1; index < plan.legs().size(); index++) {
			remaining += plan.legs().get(index).track().length();
		}
		NavigationStatus status = match.distanceToTrack() <= TRACK_MATCH_DISTANCE
				? NavigationStatus.ON_ROUTE : NavigationStatus.UNCERTAIN;
		Component instruction = status == NavigationStatus.ON_ROUTE
				? instruction(player, network, leg) : Component.translatable("railwaygps.instruction.uncertain");
		snapshot = new NavigationSnapshot(
				name(network, plan.destination()),
				name(network, leg.to()),
				instruction,
				distanceToNext,
				remaining,
				plan.legs().size() - currentLeg - 1,
				status);
	}

	private static NavigationSnapshot uncertain(RailwayNetwork network, PhysicalRouteLeg leg) {
		return new NavigationSnapshot(name(network, plan.destination()), name(network, leg.to()),
				Component.translatable("railwaygps.instruction.uncertain"),
				leg.track().length(), remainingPlannedDistance(), plan.legs().size() - currentLeg - 1, NavigationStatus.UNCERTAIN);
	}

	private static Component instruction(LocalPlayer player, RailwayNetwork network, PhysicalRouteLeg leg) {
		PhysicalInstructionResolver.Instruction resolved;
		if (currentLeg == 0 && isInsideControlPoint(player, network, leg.from())) {
			Vec3 heading = player.getVehicle() != null ? player.getVehicle().getDeltaMovement() : Vec3.ZERO;
			if (heading.x() * heading.x() + heading.z() * heading.z() < 0.0025) {
				heading = player.getLookAngle();
			}
			resolved = PhysicalInstructionResolver.fromStart(leg, heading.x(), heading.z());
		} else if (currentLeg + 1 < plan.legs().size()) {
			resolved = PhysicalInstructionResolver.between(network, leg, plan.legs().get(currentLeg + 1));
		} else {
			return Component.translatable("railwaygps.instruction.continue", name(network, leg.to()));
		}
		return switch (resolved.direction()) {
			case CUSTOM -> Component.literal(resolved.customText());
			case STRAIGHT -> Component.translatable("railwaygps.instruction.straight");
			case LEFT -> Component.translatable("railwaygps.instruction.left");
			case RIGHT -> Component.translatable("railwaygps.instruction.right");
			case BACK -> Component.translatable("railwaygps.instruction.back");
			case UNKNOWN -> Component.translatable("railwaygps.instruction.continue", name(network, leg.to()));
		};
	}

	private static double remainingPlannedDistance() {
		double distance = 0;
		for (int index = currentLeg; index < plan.legs().size(); index++) {
			distance += plan.legs().get(index).track().length();
		}
		return distance;
	}

	private static void replanAfterNetworkChange(LocalPlayer player) {
		String destination = plan.destination();
		RailwayNetwork network = ClientRailwayNetwork.network();
		Optional<String> start = nearestNode(player, network);
		Optional<PhysicalRoutePlan> replacement = start.flatMap(node -> PhysicalRoutePlanner.shortestPath(network, node, destination));
		if (replacement.isPresent()) {
			plan = replacement.get();
			currentLeg = 0;
			revision++;
		} else {
			stop();
		}
		networkVersion = ClientRailwayNetwork.version();
	}

	private static Optional<String> nearestNode(LocalPlayer player, RailwayNetwork network) {
		String dimension = player.level().dimension().identifier().toString();
		return network.controlPoints().values().stream()
				.filter(point -> dimension.equals(point.zone().dimension()))
				.filter(point -> point.zone().contains(player.getX(), player.getY(), player.getZ()))
				.min(Comparator.comparingDouble(point -> distanceSquared(player.position(), point.zone().boxes().getFirst())))
				.map(ControlPoint::id);
	}

	private static boolean isInsideControlPoint(LocalPlayer player, RailwayNetwork network, String id) {
		ControlPoint point = network.controlPoints().get(id);
		return point != null && point.zone().dimension().equals(player.level().dimension().identifier().toString())
				&& point.zone().containsForExit(player.getX(), player.getY(), player.getZ());
	}

	private static String name(RailwayNetwork network, String id) {
		ControlPoint point = network.controlPoints().get(id);
		return point == null ? id : point.name();
	}

	private static double distanceSquared(Vec3 position, ZoneBox box) {
		double x = (box.min().x() + box.max().x() + 1) / 2.0;
		double y = (box.min().y() + box.max().y() + 1) / 2.0;
		double z = (box.min().z() + box.max().z() + 1) / 2.0;
		return square(position.x() - x) + square(position.y() - y) + square(position.z() - z);
	}

	private static double square(double value) {
		return value * value;
	}

	public record StartResult(boolean success, Component message) {
	}
}
