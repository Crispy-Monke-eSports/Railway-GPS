package se.railwaygps.navigation;

import se.railwaygps.model.RailwayNetwork;
import se.railwaygps.model.Route;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;

/** Computes the shortest permitted route by recorded railway distance. */
public final class RoutePlanner {
	private RoutePlanner() {
	}

	public static Optional<RoutePlan> shortestPath(RailwayNetwork network, String start, String destination) {
		if (!network.containsNode(start) || !network.containsNode(destination)) {
			return Optional.empty();
		}
		if (start.equals(destination)) {
			return Optional.of(new RoutePlan(start, destination, List.of(), 0));
		}

		Map<String, List<RouteLeg>> outgoing = buildOutgoing(network);
		Map<String, Double> distances = new HashMap<>();
		Map<String, RouteLeg> previous = new HashMap<>();
		PriorityQueue<NodeDistance> queue = new PriorityQueue<>(Comparator.comparingDouble(NodeDistance::distance));
		distances.put(start, 0.0);
		queue.add(new NodeDistance(start, 0));

		while (!queue.isEmpty()) {
			NodeDistance current = queue.remove();
			if (current.distance() > distances.getOrDefault(current.node(), Double.POSITIVE_INFINITY)) {
				continue;
			}
			if (current.node().equals(destination)) {
				return Optional.of(buildPlan(start, destination, current.distance(), previous));
			}
			for (RouteLeg leg : outgoing.getOrDefault(current.node(), List.of())) {
				double candidate = current.distance() + leg.route().distance();
				if (candidate < distances.getOrDefault(leg.to(), Double.POSITIVE_INFINITY)) {
					distances.put(leg.to(), candidate);
					previous.put(leg.to(), leg);
					queue.add(new NodeDistance(leg.to(), candidate));
				}
			}
		}
		return Optional.empty();
	}

	private static Map<String, List<RouteLeg>> buildOutgoing(RailwayNetwork network) {
		Map<String, List<RouteLeg>> outgoing = new HashMap<>();
		for (Route route : network.routes().values()) {
			outgoing.computeIfAbsent(route.from(), ignored -> new ArrayList<>())
					.add(new RouteLeg(route, route.from(), route.to(), false));
			if (route.bidirectional()) {
				outgoing.computeIfAbsent(route.to(), ignored -> new ArrayList<>())
						.add(new RouteLeg(route, route.to(), route.from(), true));
			}
		}
		return outgoing;
	}

	private static RoutePlan buildPlan(
			String start, String destination, double distance, Map<String, RouteLeg> previous) {
		List<RouteLeg> reversed = new ArrayList<>();
		String node = destination;
		while (!node.equals(start)) {
			RouteLeg leg = previous.get(node);
			if (leg == null) {
				throw new IllegalStateException("Incomplete route predecessor chain");
			}
			reversed.add(leg);
			node = leg.from();
		}
		return new RoutePlan(start, destination, reversed.reversed(), distance);
	}

	private record NodeDistance(String node, double distance) {
	}
}
