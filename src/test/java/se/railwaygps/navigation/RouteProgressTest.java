package se.railwaygps.navigation;

import org.junit.jupiter.api.Test;
import se.railwaygps.model.TrackPoint;
import se.railwaygps.railway.model.ControlPoint;
import se.railwaygps.railway.model.PhysicalTrack;
import se.railwaygps.railway.model.RailwayNetwork;
import se.railwaygps.railway.model.TrackDirection;
import se.railwaygps.railway.model.Zone;
import se.railwaygps.railway.model.ZoneBox;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RouteProgressTest {
	@Test void measuresAlongRailsToNextRedlightAndReportsNoneOnDirectStationLeg() {
		PhysicalRouteLeg first = leg("first", "a", "signal", 10);
		PhysicalRouteLeg second = leg("second", "signal", "station", 20);
		RailwayNetwork network = new RailwayNetwork(1, Map.of(
				"signal", point("signal", ControlPoint.Kind.REDLIGHT),
				"station", point("station", ControlPoint.Kind.STATION)), Map.of(), Map.of(), Map.of(), Map.of());
		PhysicalRoutePlan plan = new PhysicalRoutePlan("a", "station", List.of(first, second), 30);

		assertEquals(4, RouteProgress.distanceToNextRedlight(network, plan, 0, 4).orElseThrow(), 0.001);
		assertTrue(RouteProgress.distanceToNextRedlight(network, plan, 1, 12).isEmpty());
	}

	private static PhysicalRouteLeg leg(String id, String from, String to, double length) {
		return new PhysicalRouteLeg(new PhysicalTrack(id, "section", from, to, "minecraft:overworld",
				List.of(new TrackPoint(0, 64, 0), new TrackPoint((int) length, 64, 0)), length,
				Set.of(TrackDirection.FORWARD), null, PhysicalTrack.Status.VERIFIED), TrackDirection.FORWARD);
	}

	private static ControlPoint point(String id, ControlPoint.Kind kind) {
		return new ControlPoint(id, id, kind, new Zone("minecraft:overworld", List.of(
				new ZoneBox(new TrackPoint(0, 60, 0), new TrackPoint(1, 70, 1))), 1), Map.of());
	}
}
