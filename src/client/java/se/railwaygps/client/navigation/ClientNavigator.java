package se.railwaygps.client.navigation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.phys.Vec3;
import se.railwaygps.client.ClientRailwayNetwork;
import se.railwaygps.model.TrackPoint;
import se.railwaygps.navigation.PolylineMatcher;
import se.railwaygps.navigation.PhysicalInstructionResolver;
import se.railwaygps.navigation.PhysicalRouteLeg;
import se.railwaygps.navigation.PhysicalRoutePlan;
import se.railwaygps.navigation.PhysicalRoutePlanner;
import se.railwaygps.navigation.RailMatchConfidence;
import se.railwaygps.navigation.StartInstructionResolver;
import se.railwaygps.navigation.StationDestination;
import se.railwaygps.railway.model.ControlPoint;
import se.railwaygps.railway.model.PhysicalTrack;
import se.railwaygps.railway.model.RailwayNetwork;
import se.railwaygps.railway.model.TraversalRef;
import se.railwaygps.railway.model.ZoneOccupancyTracker;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.HashMap;
import java.util.Map;

public final class ClientNavigator {
	private static final double TRACK_MATCH_DISTANCE = 6.0;
	private static final double START_TRACK_MATCH_DISTANCE = 2.0;
	private static final double REDLIGHT_APPROACH_DISTANCE = 32.0;
	private static final double MINIMUM_DIRECTION_SPEED = 0.02;
	private static final double MINIMUM_DIRECTION_ALIGNMENT = 0.65;
	private static PhysicalRoutePlan plan;
	private static int currentLeg;
	private static String networkVersion = "";
	private static NavigationSnapshot snapshot;
	private static long revision;
	private static String pendingDestination;
	private static int stableMovementTicks;
	private static boolean pendingWasMounted;
	private static boolean pendingNoRoute;
	private static boolean activeWhileMounted;
	private static final Map<String, ZoneOccupancyTracker> zoneTrackers = new HashMap<>();

	private ClientNavigator() {
	}

	public static StartResult start(LocalPlayer player, String destination) {
		RailwayNetwork network = ClientRailwayNetwork.network();
		if (StationDestination.status(network, destination,
				player.level().dimension().identifier().toString()) != StationDestination.Status.AVAILABLE) {
			return new StartResult(false, Component.translatable("railwaygps.nav.start.unknown_destination", destination));
		}
		pendingDestination = destination;
		plan = null;
		currentLeg = 0;
		stableMovementTicks = 0;
		pendingWasMounted = false;
		pendingNoRoute = false;
		boolean mounted = player.getVehicle() instanceof AbstractMinecart;
		if (!mounted || player.getVehicle().getDeltaMovement().length() < MINIMUM_DIRECTION_SPEED) {
			snapshot = pending(network, destination, false);
			revision++;
			return new StartResult(true, Component.translatable("railwaygps.nav.start.pending", name(network, destination)));
		}
		Optional<PhysicalRoutePlan> calculated = planFromCurrentTrack(player, network, destination);
		if (calculated.isEmpty()) {
			pendingNoRoute = true;
			snapshot = pending(network, destination, true);
			revision++;
			return new StartResult(true, Component.translatable("railwaygps.nav.start.pending",
					name(network, destination)));
		}
		plan = calculated.get();
		pendingDestination = null;
		activeWhileMounted = true;
		currentLeg = 0;
		zoneTrackers.clear();
		networkVersion = ClientRailwayNetwork.version();
		revision++;
		update(player);
		return new StartResult(true, Component.translatable("railwaygps.nav.start.started", name(network, destination)));
	}

	public static void stop() {
		boolean wasActive = plan != null || pendingDestination != null || snapshot != null;
		plan = null;
		pendingDestination = null;
		snapshot = null;
		currentLeg = 0;
		zoneTrackers.clear();
		stableMovementTicks = 0;
		pendingWasMounted = false;
		pendingNoRoute = false;
		activeWhileMounted = false;
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
		if (client.player == null) {
			return;
		}
		boolean mounted = client.player.getVehicle() instanceof AbstractMinecart;
		if (plan != null && activeWhileMounted && !mounted) { stop(); return; }
		if (plan == null && pendingDestination != null) {
			RailwayNetwork network = ClientRailwayNetwork.network();
			if (StationDestination.status(network, pendingDestination,
					client.player.level().dimension().identifier().toString()) != StationDestination.Status.AVAILABLE) {
				client.player.sendSystemMessage(Component.translatable("railwaygps.nav.station_removed"));
				stop();
				return;
			}
			if (!mounted) {
				if (pendingWasMounted) stop();
				return;
			}
			pendingWasMounted = true;
			if (client.player.getVehicle().getDeltaMovement().length() < MINIMUM_DIRECTION_SPEED) {
				stableMovementTicks = 0;
				if (!pendingNoRoute) snapshot = pending(network, pendingDestination, false);
				return;
			}
			if (++stableMovementTicks < 3) return;
			Optional<PhysicalRoutePlan> pending = planFromCurrentTrack(client.player,
					ClientRailwayNetwork.network(), pendingDestination);
			if (pending.isPresent()) {
				plan = pending.get();
				pendingDestination = null;
				pendingNoRoute = false;
				currentLeg = 0;
				activeWhileMounted = true;
				networkVersion = ClientRailwayNetwork.version();
				revision++;
				update(client.player);
			} else {
				stableMovementTicks = 0;
				pendingNoRoute = true;
				snapshot = pending(network, pendingDestination, true);
			}
			return;
		}
		if (plan == null) return;
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
		if (confirmedInsideControlPoint(player, network, plan.destination())) {
			currentLeg = plan.legs().size();
			snapshot = new NavigationSnapshot(name(network, plan.destination()), name(network, plan.destination()),
					Component.translatable("railwaygps.instruction.arrived"),
					0, 0, 0, NavigationStatus.ARRIVED);
			return;
		}
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
		Vec3 movement = player.getVehicle() == null ? Vec3.ZERO : player.getVehicle().getDeltaMovement();
		if (match == null || movement.length() >= MINIMUM_DIRECTION_SPEED
				&& match.movementAlignment(movement.x, movement.y, movement.z) < MINIMUM_DIRECTION_ALIGNMENT
				|| match != null && hasCompetingTrack(network, leg.track().id(), leg.track().dimension(), position, movement,
						match.distanceToTrack())) {
			snapshot = uncertain(network, leg);
			return;
		}

		double geometryRemaining = Math.max(0, match.trackLength() - match.distanceAlong());
		double distanceToNext = match.trackLength() == 0
				? leg.track().length()
				: geometryRemaining * leg.track().length() / match.trackLength();
		if (match.distanceToTrack() <= TRACK_MATCH_DISTANCE && confirmedInsideControlPoint(player, network, leg.to())
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
				? instruction(player, network, leg, distanceToNext) : Component.translatable("railwaygps.instruction.uncertain");
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
				Double.NaN, Double.NaN, 0, NavigationStatus.UNCERTAIN);
	}

	private static NavigationSnapshot pending(RailwayNetwork network, String destination, boolean noRoute) {
		return new NavigationSnapshot(name(network, destination), name(network, destination),
				Component.translatable(noRoute ? "railwaygps.instruction.no_route"
						: "railwaygps.instruction.waiting_direction"),
				Double.NaN, Double.NaN, 0, NavigationStatus.UNCERTAIN);
	}

	private static Component instruction(LocalPlayer player, RailwayNetwork network, PhysicalRouteLeg leg,
			double distanceToNext) {
		PhysicalInstructionResolver.Instruction resolved;
		if (currentLeg == 0 && isInsideControlPoint(player, network, leg.from())) {
			Vec3 position = player.getVehicle() != null ? player.getVehicle().position() : player.position();
			resolved = StartInstructionResolver.resolve(network, leg, position.x(), position.y(), position.z());
		} else if (currentLeg + 1 < plan.legs().size()) {
			if (distanceToNext > REDLIGHT_APPROACH_DISTANCE && !isInsideControlPoint(player, network, leg.to())) {
				return Component.translatable("railwaygps.instruction.continue", name(network, leg.to()));
			}
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

	private static void replanAfterNetworkChange(LocalPlayer player) {
		String destination = plan.destination();
		RailwayNetwork network = ClientRailwayNetwork.network();
		if (StationDestination.status(network, destination,
				player.level().dimension().identifier().toString()) != StationDestination.Status.AVAILABLE) {
			player.sendSystemMessage(Component.translatable("railwaygps.nav.station_removed"));
			stop();
			return;
		}
		Optional<PhysicalRoutePlan> replacement = planFromCurrentTrack(player, network, destination);
		if (replacement.isPresent()) {
			plan = replacement.get();
			currentLeg = 0;
			revision++;
		} else {
			plan = null;
			pendingDestination = destination;
			pendingNoRoute = true;
			pendingWasMounted = player.getVehicle() instanceof AbstractMinecart;
			stableMovementTicks = 0;
			activeWhileMounted = false;
			snapshot = pending(network, destination, true);
			revision++;
		}
		networkVersion = ClientRailwayNetwork.version();
	}

	private static Optional<PhysicalRoutePlan> planFromCurrentTrack(
			LocalPlayer player, RailwayNetwork network, String destination) {
		String dimension = player.level().dimension().identifier().toString();
		Vec3 position = player.getVehicle() != null ? player.getVehicle().position() : player.position();
		Vec3 movement = player.getVehicle() != null ? player.getVehicle().getDeltaMovement() : Vec3.ZERO;
		if (movement.length() < MINIMUM_DIRECTION_SPEED) return Optional.empty();
		List<StartingRoute> candidates = network.tracks().values().stream()
				.filter(track -> track.status() == PhysicalTrack.Status.VERIFIED)
				.filter(track -> dimension.equals(track.dimension()))
				.flatMap(track -> track.directions().stream().map(direction -> {
					PhysicalRouteLeg leg = new PhysicalRouteLeg(track, direction);
					PolylineMatcher.Match match = PolylineMatcher.nearest(leg.points(),
							position.x(), position.y(), position.z()).orElse(null);
					if (match != null && match.movementAlignment(movement.x, movement.y, movement.z)
							< MINIMUM_DIRECTION_ALIGNMENT) match = null;
					double geometryRemaining = match == null ? track.length()
							: Math.max(0, match.trackLength() - match.distanceAlong())
							* track.length() / Math.max(0.001, match.trackLength());
					Optional<PhysicalRoutePlan> route = PhysicalRoutePlanner.continueFromPosition(network,
							new TraversalRef(track.id(), direction), destination, geometryRemaining);
					return new StartingRoute(match, route.orElse(null));
				}))
				.filter(candidate -> candidate.match != null)
				.toList();
		double nearest = candidates.stream().mapToDouble(candidate -> candidate.match.distanceToTrack())
				.min().orElse(Double.POSITIVE_INFINITY);
		List<StartingRoute> eligible = candidates.stream()
				.filter(candidate -> candidate.plan != null && nearest <= START_TRACK_MATCH_DISTANCE
						&& candidate.match.distanceToTrack() <= nearest + 0.15)
				.toList();
		Optional<String> uniqueTrack = RailMatchConfidence.uniqueTrack(eligible.stream()
				.map(candidate -> new RailMatchConfidence.Candidate(
						candidate.plan.legs().getFirst().track().id(), candidate.match.distanceToTrack(), true))
				.toList(), START_TRACK_MATCH_DISTANCE, 0.15);
		if (uniqueTrack.isEmpty()) return Optional.empty();
		return eligible.stream()
				.filter(candidate -> candidate.plan.legs().getFirst().track().id().equals(uniqueTrack.get()))
				.min(Comparator.comparingDouble((StartingRoute candidate) -> candidate.match.distanceToTrack())
						.thenComparingDouble(candidate -> candidate.plan.distance()))
				.map(StartingRoute::plan);
	}

	private static boolean hasCompetingTrack(RailwayNetwork network, String currentTrackId, String dimension, Vec3 position,
			Vec3 movement, double currentDistance) {
		return network.tracks().values().stream()
				.filter(track -> track.status() == PhysicalTrack.Status.VERIFIED
						&& track.dimension().equals(dimension) && !track.id().equals(currentTrackId))
				.flatMap(track -> track.directions().stream().map(direction -> PolylineMatcher.nearest(
						direction == se.railwaygps.railway.model.TrackDirection.FORWARD
								? track.points() : track.points().reversed(),
						position.x(), position.y(), position.z()).orElse(null)))
				.anyMatch(candidate -> candidate != null && candidate.distanceToTrack() <= currentDistance + 0.15
						&& (movement.length() < MINIMUM_DIRECTION_SPEED
						|| candidate.movementAlignment(movement.x, movement.y, movement.z) >= MINIMUM_DIRECTION_ALIGNMENT));
	}

	private static boolean isInsideControlPoint(LocalPlayer player, RailwayNetwork network, String id) {
		ControlPoint point = network.controlPoints().get(id);
		return point != null && point.zone().dimension().equals(player.level().dimension().identifier().toString())
				&& point.zone().containsForExit(player.getX(), player.getY(), player.getZ());
	}

	private static boolean confirmedInsideControlPoint(LocalPlayer player, RailwayNetwork network, String id) {
		ControlPoint point = network.controlPoints().get(id);
		if (point == null || !point.zone().dimension().equals(
				player.level().dimension().identifier().toString())) return false;
		ZoneOccupancyTracker tracker = zoneTrackers.computeIfAbsent(point.zone().id(), ignored -> new ZoneOccupancyTracker());
		tracker.observe(point.zone(), player.getX(), player.getY(), player.getZ());
		return tracker.inside();
	}

	private static String name(RailwayNetwork network, String id) {
		ControlPoint point = network.controlPoints().get(id);
		return point == null ? id : point.name();
	}

	private record StartingRoute(PolylineMatcher.Match match, PhysicalRoutePlan plan) { }

	public record StartResult(boolean success, Component message) {
	}
}
