package se.railwaygps.railway.model;

import org.junit.jupiter.api.Test;
import se.railwaygps.model.NetworkValidationException;
import se.railwaygps.model.TrackPoint;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RailwayNetworkValidatorTest {
	@Test
	void stationNamesAreUniqueAfterTrimmingAndCaseFoldingAcrossDimensions() {
		RailwayNetwork network = new RailwayNetwork(1, Map.of(
				"one", station("one", " Central ", "minecraft:overworld", 0),
				"two", station("two", "central", "minecraft:the_nether", 20)),
				Map.of(), Map.of(), Map.of(), Map.of());

		NetworkValidationException exception = assertThrows(NetworkValidationException.class,
				() -> RailwayNetworkValidator.validate(network));
		assertTrue(exception.errors().stream().anyMatch(error -> error.contains("same display name")));
	}

	@Test
	void multipleIndependentRailsInsideAStationZoneAreValid() {
		ControlPoint station = new ControlPoint("central", "Central", ControlPoint.Kind.STATION,
				new Zone("minecraft:overworld", List.of(new ZoneBox(point(0, 60, 0), point(10, 70, 10))), 1), Map.of());
		RailwayNetwork network = new RailwayNetwork(1, Map.of("central", station),
				Map.of(), Map.of(), Map.of(), Map.of());

		assertDoesNotThrow(() -> RailwayNetworkValidator.validate(network));
	}

	@Test
	void reportsTrackTopologyDimensionAndGeometryLengthTogether() {
		Map<String, ControlPoint> points = Map.of(
				"a", station("a", "A", "minecraft:overworld", 0),
				"b", station("b", "B", "minecraft:overworld", 10),
				"c", station("c", "C", "minecraft:overworld", 20));
		PhysicalTrack bad = new PhysicalTrack("bad", "section", "a", "c", "minecraft:the_nether",
				List.of(point(0, 64, 0), point(10, 64, 0)), 99,
				Set.of(TrackDirection.FORWARD), null, PhysicalTrack.Status.VERIFIED);
		RailwayNetwork network = new RailwayNetwork(1, points,
				Map.of("section", new TrackSection("section", "a", "b", Set.of("bad"))),
				Map.of("bad", bad), Map.of(), Map.of());

		NetworkValidationReport report = RailwayNetworkValidator.inspect(network);
		assertTrue(report.errors().stream().anyMatch(error -> error.contains("endpoints do not match")));
		assertTrue(report.errors().stream().anyMatch(error -> error.contains("dimension")));
		assertTrue(report.errors().stream().anyMatch(error -> error.contains("stored length")));
	}

	@Test
	void enabledTransitionAndLabelCannotUseUnverifiedTrackStateOrDirection() {
		Map<String, ControlPoint> points = Map.of(
				"a", station("a", "A", "minecraft:overworld", 0),
				"b", station("b", "B", "minecraft:overworld", 10),
				"c", station("c", "C", "minecraft:overworld", 20));
		PhysicalTrack ab = track("ab", "sab", "a", "b", 0, 10, PhysicalTrack.Status.DISABLED);
		PhysicalTrack bc = track("bc", "sbc", "b", "c", 10, 20, PhysicalTrack.Status.VERIFIED);
		RailwayNetwork network = new RailwayNetwork(1, points,
				Map.of("sab", new TrackSection("sab", "a", "b", Set.of("ab")),
						"sbc", new TrackSection("sbc", "b", "c", Set.of("bc"))),
				Map.of("ab", ab, "bc", bc),
				Map.of("through", new TrackTransition("through", "b",
						new TraversalRef("ab", TrackDirection.FORWARD),
						new TraversalRef("bc", TrackDirection.FORWARD), true, null)),
				Map.of("name", new TrackLabel("name", "Name", TrackLabel.Type.WAY, 1, 0xFFFFFF,
						List.of(new TrackRange("ab", 0, 5, TrackDirection.REVERSE)))));

		NetworkValidationReport report = RailwayNetworkValidator.inspect(network);
		assertTrue(report.errors().stream().anyMatch(error -> error.contains("non-verified track")));
		assertTrue(report.errors().stream().anyMatch(error -> error.contains("unverified direction")));
	}

	@Test
	void overlapsAndExactDuplicateGeometryAreVisibleWarningsNotBlockingErrors() {
		ControlPoint a = station("a", "A", "minecraft:overworld", 0);
		ControlPoint b = station("b", "B", "minecraft:overworld", 0);
		PhysicalTrack one = track("one", "section", "a", "b", 0, 10, PhysicalTrack.Status.VERIFIED);
		PhysicalTrack two = new PhysicalTrack("two", "section", "b", "a", "minecraft:overworld",
				one.points().reversed(), 10, Set.of(TrackDirection.FORWARD), null, PhysicalTrack.Status.VERIFIED);
		RailwayNetwork network = new RailwayNetwork(1, Map.of("a", a, "b", b),
				Map.of("section", new TrackSection("section", "a", "b", Set.of("one", "two"))),
				Map.of("one", one, "two", two), Map.of(), Map.of());

		NetworkValidationReport report = RailwayNetworkValidator.inspect(network);
		assertTrue(report.valid());
		assertFalse(report.warnings().isEmpty());
		assertTrue(report.warnings().stream().anyMatch(warning -> warning.contains("overlap")));
		assertTrue(report.warnings().stream().anyMatch(warning -> warning.contains("duplicate physical geometry")));
	}

	private static ControlPoint station(String id, String name, String dimension, int x) {
		return new ControlPoint(id, name, ControlPoint.Kind.STATION,
				new Zone(dimension, List.of(new ZoneBox(point(x, 63, -1), point(x + 2, 66, 1))), 1), Map.of());
	}

	private static PhysicalTrack track(String id, String section, String from, String to,
			int start, int end, PhysicalTrack.Status status) {
		return new PhysicalTrack(id, section, from, to, "minecraft:overworld",
				List.of(point(start, 64, 0), point(end, 64, 0)), Math.abs(end - start),
				Set.of(TrackDirection.FORWARD), null, status);
	}

	private static TrackPoint point(int x, int y, int z) { return new TrackPoint(x, y, z); }
}
