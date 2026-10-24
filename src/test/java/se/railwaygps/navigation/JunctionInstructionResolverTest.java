package se.railwaygps.navigation;

import org.junit.jupiter.api.Test;
import se.railwaygps.model.Junction;
import se.railwaygps.model.RailwayNetwork;
import se.railwaygps.model.Route;
import se.railwaygps.model.Station;
import se.railwaygps.model.TrackPoint;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JunctionInstructionResolverTest {
	private static final String DIMENSION = "minecraft:overworld";
	private static final Route INCOMING = route("incoming", "north", "junction", point(0, -10), point(0, 0));

	@Test
	void classifiesRecordedTrackGeometryRelativeToArrivalDirection() {
		assertEquals(JunctionInstructionResolver.Direction.STRAIGHT,
				resolve(route("straight", "junction", "south", point(0, 0), point(0, 10))).direction());
		assertEquals(JunctionInstructionResolver.Direction.LEFT,
				resolve(route("left", "junction", "east", point(0, 0), point(10, 0))).direction());
		assertEquals(JunctionInstructionResolver.Direction.RIGHT,
				resolve(route("right", "junction", "west", point(0, 0), point(-10, 0))).direction());
		assertEquals(JunctionInstructionResolver.Direction.BACK,
				resolve(route("back", "junction", "north", point(0, 0), point(0, -10))).direction());
	}

	@Test
	void customTransitionInstructionOverridesGeometry() {
		Route outgoing = route("island", "junction", "east", point(0, 0), point(10, 0));
		RailwayNetwork network = network(outgoing, Map.of("incoming>island", "Välj knappen för Martyrön"));

		JunctionInstructionResolver.Instruction result = JunctionInstructionResolver.between(
				network, new RouteLeg(INCOMING, "north", "junction", false),
				new RouteLeg(outgoing, "junction", "east", false));

		assertEquals(JunctionInstructionResolver.Direction.CUSTOM, result.direction());
		assertEquals("Välj knappen för Martyrön", result.customText());
	}

	@Test
	void supportsCustomInstructionAtJourneyStart() {
		Route outgoing = route("island", "junction", "east", point(0, 0), point(10, 0));
		RailwayNetwork network = network(outgoing, Map.of("*>island", "Tryck på ö-knappen"));

		JunctionInstructionResolver.Instruction result = JunctionInstructionResolver.fromStart(
				network, new RouteLeg(outgoing, "junction", "east", false), 0, 1);

		assertEquals(JunctionInstructionResolver.Direction.CUSTOM, result.direction());
		assertEquals("Tryck på ö-knappen", result.customText());
	}

	private static JunctionInstructionResolver.Instruction resolve(Route outgoing) {
		return JunctionInstructionResolver.between(
				network(outgoing, Map.of()), new RouteLeg(INCOMING, "north", "junction", false),
				new RouteLeg(outgoing, "junction", outgoing.to(), false));
	}

	private static RailwayNetwork network(Route outgoing, Map<String, String> instructions) {
		Map<String, Station> stations = new LinkedHashMap<>();
		stations.put("north", station("north", 0, -10));
		stations.put(outgoing.to(), station(outgoing.to(), outgoing.points().getLast().x(), outgoing.points().getLast().z()));
		Junction junction = new Junction("junction", "Junction", DIMENSION, point(0, 0), 25, instructions);
		return new RailwayNetwork(stations, Map.of("junction", junction), Map.of(
				INCOMING.id(), INCOMING, outgoing.id(), outgoing));
	}

	private static Station station(String id, int x, int z) {
		return new Station(id, id, DIMENSION, new TrackPoint(x, 64, z));
	}

	private static Route route(String id, String from, String to, TrackPoint... points) {
		return new Route(id, id, from, to, DIMENSION, false, 10, List.of(points));
	}

	private static TrackPoint point(int x, int z) {
		return new TrackPoint(x, 64, z);
	}
}
