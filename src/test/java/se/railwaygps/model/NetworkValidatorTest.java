package se.railwaygps.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NetworkValidatorTest {
	private static final Station START = new Station(
			"start", "Start", "minecraft:overworld", new TrackPoint(0, 64, 0));
	private static final Junction END = new Junction(
			"end", "End", "minecraft:overworld", new TrackPoint(10, 64, 0), 25);

	@Test
	void acceptsValidDirectedNetwork() {
		Route route = new Route("main_line", "Main line", "start", "end", "minecraft:overworld", false, 10,
				List.of(new TrackPoint(0, 64, 0), new TrackPoint(10, 64, 0)));
		RailwayNetwork network = new RailwayNetwork(
				Map.of(START.id(), START), Map.of(END.id(), END), Map.of(route.id(), route));

		assertDoesNotThrow(() -> NetworkValidator.validate(network));
	}

	@Test
	void rejectsDuplicateIdsAcrossNodeTypes() {
		Junction duplicate = new Junction("start", "Duplicate", "minecraft:overworld", new TrackPoint(1, 64, 0), 25);
		RailwayNetwork network = new RailwayNetwork(Map.of("start", START), Map.of("start", duplicate), Map.of());

		NetworkValidationException exception = assertThrows(
				NetworkValidationException.class, () -> NetworkValidator.validate(network));
		assertTrue(exception.getMessage().contains("Duplicate node id 'start'"));
	}

	@Test
	void rejectsRouteWithMissingEndpoint() {
		Route route = new Route("broken", "Broken", "start", "missing", "minecraft:overworld", false, 10,
				List.of(new TrackPoint(0, 64, 0), new TrackPoint(10, 64, 0)));
		RailwayNetwork network = new RailwayNetwork(Map.of("start", START), Map.of(), Map.of("broken", route));

		NetworkValidationException exception = assertThrows(
				NetworkValidationException.class, () -> NetworkValidator.validate(network));
		assertTrue(exception.getMessage().contains("missing end node 'missing'"));
	}

	@Test
	void validatesSafeIds() {
		assertTrue(NetworkValidator.isValidId("norra_bron-2"));
		assertTrue(!NetworkValidator.isValidId("../world"));
		assertTrue(!NetworkValidator.isValidId("Norra bron"));
	}
}
