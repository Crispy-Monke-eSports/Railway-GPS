package se.railwaygps.navigation;

import org.junit.jupiter.api.Test;
import se.railwaygps.model.RailwayNetwork;
import se.railwaygps.model.Route;
import se.railwaygps.model.Station;
import se.railwaygps.model.TrackPoint;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoutePlannerTest {
	@Test
	void choosesShortestParallelPath() {
		RailwayNetwork network = network(
				route("direct", "a", "c", 30, false),
				route("a_b", "a", "b", 10, false),
				route("b_c", "b", "c", 10, false));

		RoutePlan plan = RoutePlanner.shortestPath(network, "a", "c").orElseThrow();

		assertEquals(List.of("a_b", "b_c"), plan.legs().stream().map(leg -> leg.route().id()).toList());
		assertEquals(20, plan.distance());
	}

	@Test
	void directedRouteCannotBeUsedBackwards() {
		RailwayNetwork network = network(route("one_way", "a", "b", 10, false));

		assertTrue(RoutePlanner.shortestPath(network, "a", "b").isPresent());
		assertFalse(RoutePlanner.shortestPath(network, "b", "a").isPresent());
	}

	@Test
	void explicitlyBidirectionalRouteCanBeReversed() {
		RailwayNetwork network = network(route("two_way", "a", "b", 10, true));

		RouteLeg leg = RoutePlanner.shortestPath(network, "b", "a").orElseThrow().legs().getFirst();
		assertTrue(leg.reversed());
		assertEquals("b", leg.from());
		assertEquals("a", leg.to());
	}

	private static RailwayNetwork network(Route... routes) {
		Map<String, Station> stations = Map.of(
				"a", station("a", 0),
				"b", station("b", 10),
				"c", station("c", 20));
		return new RailwayNetwork(stations, Map.of(),
				java.util.Arrays.stream(routes).collect(java.util.stream.Collectors.toMap(Route::id, route -> route)));
	}

	private static Station station(String id, int x) {
		return new Station(id, id.toUpperCase(), "minecraft:overworld", new TrackPoint(x, 64, 0));
	}

	private static Route route(String id, String from, String to, double distance, boolean bidirectional) {
		return new Route(id, id, from, to, "minecraft:overworld", bidirectional, distance,
				List.of(new TrackPoint(0, 64, 0), new TrackPoint(1, 64, 0)));
	}
}
