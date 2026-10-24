package se.railwaygps.navigation;

import se.railwaygps.railway.model.RailwayNetworkValidator;
import se.railwaygps.railway.model.PhysicalTrack;
import se.railwaygps.railway.model.RailwayNetwork;
import se.railwaygps.railway.model.TrackDirection;
import se.railwaygps.railway.model.TrackTransition;
import se.railwaygps.railway.model.TraversalRef;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
		return search(network, start, destination, initial);
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
				Map.of(current, track.length()));
	}

	private static Optional<PhysicalRoutePlan> search(RailwayNetwork network, String start, String destination,
			Map<TraversalRef, Double> initial) {
		Map<TraversalRef, Double> distances = new HashMap<>(initial);
		Map<TraversalRef, TraversalRef> previous = new HashMap<>();
		PriorityQueue<StateDistance> queue = new PriorityQueue<>(Comparator.comparingDouble(StateDistance::distance));
		initial.forEach((traversal, distance) -> queue.add(new StateDistance(traversal, distance)));

		while (!queue.isEmpty()) {
			StateDistance current = queue.remove();
			if (current.distance() > distances.getOrDefault(current.traversal(), Double.POSITIVE_INFINITY)) continue;
			PhysicalTrack track = network.tracks().get(current.traversal().trackId());
			if (RailwayNetworkValidator.endPoint(track, current.traversal().direction()).equals(destination)) {
				return Optional.of(buildPlan(network, start, destination, current.traversal(), current.distance(), previous));
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
		return Optional.empty();
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
