package se.railwaygps.navigation;

import se.railwaygps.model.TrackGeometry;
import se.railwaygps.model.TrackPoint;
import se.railwaygps.railway.model.RailwayNetworkValidator;
import se.railwaygps.railway.model.ControlPoint;
import se.railwaygps.railway.model.PhysicalTrack;
import se.railwaygps.railway.model.RailwayNetwork;
import se.railwaygps.railway.model.TrackDirection;
import se.railwaygps.railway.model.TrackTransition;
import se.railwaygps.railway.model.TraversalRef;
import se.railwaygps.railway.model.ZoneBox;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.PriorityQueue;

/** Finds the shortest path that follows track directions and explicitly enabled transitions. */
public final class PhysicalRoutePlanner {
	private PhysicalRoutePlanner() {
	}

	public static Optional<PhysicalRoutePlan> shortestPath(
			RailwayNetwork network, String start, String destination) {
		if (!network.controlPoints().containsKey(start) || !network.controlPoints().containsKey(destination)) {
			return Optional.empty();
		}
		if (start.equals(destination)) {
			return Optional.of(new PhysicalRoutePlan(start, destination, List.of(), 0));
		}

		Map<TraversalRef, Double> initial = new HashMap<>();
		for (PhysicalTrack track : network.tracks().values()) {
			if (track.status() != PhysicalTrack.Status.VERIFIED) continue;
			for (TrackDirection direction : track.directions()) {
				if (RailwayNetworkValidator.startPoint(track, direction).equals(start)) {
					TraversalRef traversal = new TraversalRef(track.id(), direction);
					initial.put(traversal, track.length());
				}
			}
		}
		return search(network, start, destination, initial, Map.of());
	}

	/** Continues a route from the physical traversal currently occupied by the minecart. */
	public static Optional<PhysicalRoutePlan> continueFrom(
			RailwayNetwork network, TraversalRef current, String destination) {
		PhysicalTrack track = network.tracks().get(current.trackId());
		if (track == null || track.status() != PhysicalTrack.Status.VERIFIED
				|| !track.directions().contains(current.direction())
				|| !network.controlPoints().containsKey(destination)) {
			return Optional.empty();
		}
		return search(network, RailwayNetworkValidator.startPoint(track, current.direction()), destination,
				Map.of(current, track.length()), Map.of());
	}

	/** Continues from a confirmed partway position, charging only untravelled rail in the allowed direction. */
	public static Optional<PhysicalRoutePlan> continueFromPosition(
			RailwayNetwork network, TraversalRef current, String destination, double remainingOnCurrentTrack) {
		PhysicalTrack track = network.tracks().get(current.trackId());
		if (track == null || track.status() != PhysicalTrack.Status.VERIFIED
				|| !track.directions().contains(current.direction())
				|| !network.controlPoints().containsKey(destination)
				|| !Double.isFinite(remainingOnCurrentTrack) || remainingOnCurrentTrack < 0
				|| remainingOnCurrentTrack > track.length()) return Optional.empty();
		return search(network, RailwayNetworkValidator.startPoint(track, current.direction()), destination,
				Map.of(current, remainingOnCurrentTrack), Map.of(current, track.length() - remainingOnCurrentTrack));
	}

	private static Optional<PhysicalRoutePlan> search(RailwayNetwork network, String start, String destination,
			Map<TraversalRef, Double> initial, Map<TraversalRef, Double> initialOffsets) {
		Map<TraversalRef, Double> distances = new HashMap<>(initial);
		Map<TraversalRef, TraversalRef> previous = new HashMap<>();
		PriorityQueue<StateDistance> queue = new PriorityQueue<>(Comparator.comparingDouble(StateDistance::distance));
		initial.forEach((traversal, distance) -> queue.add(new StateDistance(traversal, distance)));
		TraversalRef bestArrival = null;
		double bestArrivalDistance = Double.POSITIVE_INFINITY;

		while (!queue.isEmpty()) {
			StateDistance current = queue.remove();
			if (current.distance() > distances.getOrDefault(current.traversal(), Double.POSITIVE_INFINITY)) continue;
			PhysicalTrack track = network.tracks().get(current.traversal().trackId());
			double offset = initialOffsets.getOrDefault(current.traversal(), 0.0);
			OptionalDouble arrival = arrivalDistance(network, track, current.traversal().direction(), destination, offset);
			if (arrival.isPresent()) {
				double candidate = current.distance() - track.length() + offset + arrival.getAsDouble();
				if (candidate < bestArrivalDistance) {
					bestArrival = current.traversal();
					bestArrivalDistance = candidate;
				}
			}
			for (TrackTransition transition : network.transitions().values()) {
				if (!transition.enabled() || !transition.incoming().equals(current.traversal())) continue;
				PhysicalTrack outgoing = network.tracks().get(transition.outgoing().trackId());
				if (outgoing == null || outgoing.status() != PhysicalTrack.Status.VERIFIED) continue;
				double candidate = current.distance() + outgoing.length();
				if (candidate < distances.getOrDefault(transition.outgoing(), Double.POSITIVE_INFINITY)) {
					distances.put(transition.outgoing(), candidate);
					previous.put(transition.outgoing(), current.traversal());
					queue.add(new StateDistance(transition.outgoing(), candidate));
				}
			}
		}
		return bestArrival == null ? Optional.empty()
				: Optional.of(buildPlan(network, start, destination, bestArrival, bestArrivalDistance, previous));
	}

	private static OptionalDouble arrivalDistance(RailwayNetwork network, PhysicalTrack track,
			TrackDirection direction, String destination, double offset) {
		if (RailwayNetworkValidator.endPoint(track, direction).equals(destination)) {
			return OptionalDouble.of(track.length() - offset);
		}
		ControlPoint point = network.controlPoints().get(destination);
		if (point == null || point.kind() != ControlPoint.Kind.STATION
				|| !track.dimension().equals(point.zone().dimension())) return OptionalDouble.empty();
		List<TrackPoint> geometry = direction == TrackDirection.FORWARD
				? track.points() : track.points().reversed();
		double geometryLength = TrackGeometry.length(geometry);
		if (geometryLength <= 0) return OptionalDouble.empty();
		double scale = track.length() / geometryLength;
		double travelled = 0;
		for (int index = 1; index < geometry.size(); index++) {
			TrackPoint from = geometry.get(index - 1);
			TrackPoint to = geometry.get(index);
			double dx = to.x() - from.x();
			double dy = to.y() - from.y();
			double dz = to.z() - from.z();
			double segmentLength = Math.sqrt(dx * dx + dy * dy + dz * dz);
			for (ZoneBox box : point.zone().boxes()) {
				double[] interval = segmentInterval(from, to, box);
				if (interval == null) continue;
				double entry = (travelled + segmentLength * interval[0]) * scale;
				double exit = (travelled + segmentLength * interval[1]) * scale;
				if (exit + 0.0001 >= offset) return OptionalDouble.of(Math.max(0, entry - offset));
			}
			travelled += segmentLength;
		}
		return OptionalDouble.empty();
	}

	private static double[] segmentInterval(TrackPoint from, TrackPoint to, ZoneBox box) {
		double[] interval = {0, 1};
		if (!clipAxis(from.x(), to.x() - from.x(), box.min().x(), box.max().x() + 1, interval)
				|| !clipAxis(from.y(), to.y() - from.y(), box.min().y(), box.max().y() + 1, interval)
				|| !clipAxis(from.z(), to.z() - from.z(), box.min().z(), box.max().z() + 1, interval)) return null;
		return interval;
	}

	private static boolean clipAxis(double origin, double delta, double minimum, double maximum, double[] interval) {
		if (Math.abs(delta) < 0.0000001) return origin >= minimum && origin <= maximum;
		double first = (minimum - origin) / delta;
		double second = (maximum - origin) / delta;
		if (first > second) {
			double swap = first;
			first = second;
			second = swap;
		}
		interval[0] = Math.max(interval[0], first);
		interval[1] = Math.min(interval[1], second);
		return interval[0] <= interval[1];
	}

	private static PhysicalRoutePlan buildPlan(RailwayNetwork network, String start, String destination,
			TraversalRef last, double distance, Map<TraversalRef, TraversalRef> previous) {
		List<PhysicalRouteLeg> reversed = new ArrayList<>();
		TraversalRef current = last;
		while (current != null) {
			reversed.add(new PhysicalRouteLeg(network.tracks().get(current.trackId()), current.direction()));
			current = previous.get(current);
		}
		return new PhysicalRoutePlan(start, destination, reversed.reversed(), distance);
	}

	private record StateDistance(TraversalRef traversal, double distance) {
	}
}
