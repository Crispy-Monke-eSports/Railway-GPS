package se.railwaygps.navigation;

import org.junit.jupiter.api.Test;
import se.railwaygps.model.TrackPoint;
import se.railwaygps.railway.editing.RailwayNetworkEditor;
import se.railwaygps.railway.model.ControlPoint;
import se.railwaygps.railway.model.RailwayNetwork;
import se.railwaygps.railway.model.TrackDirection;
import se.railwaygps.railway.model.TraversalRef;
import se.railwaygps.railway.model.Zone;
import se.railwaygps.railway.model.ZoneBox;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhysicalRoutePlannerTest {
	@Test
	void reachesAStationAroundAnyVerifiedThroughTrackWithoutInventingAConnection() throws Exception {
		RailwayNetwork network = baseNetwork();
		network = RailwayNetworkEditor.addControlPoint(network, "mid", "Mid", ControlPoint.Kind.STATION,
				new Zone("minecraft:overworld", List.of(new ZoneBox(point(4), point(6))), 1));

		PhysicalRoutePlan plan = PhysicalRoutePlanner.shortestPath(network, "a", "mid").orElseThrow();

		assertEquals(List.of("a_b"), plan.legs().stream().map(leg -> leg.track().id()).toList());
		assertEquals(4, plan.distance(), 0.0001);
		assertTrue(network.transitions().isEmpty());
		assertTrue(PhysicalRoutePlanner.shortestPath(network, "b", "mid").isEmpty());
	}

	@Test
	void partwayStartCannotReachAStationThatIsBehindItsPermittedDirection() throws Exception {
		RailwayNetwork network = baseNetwork();
		network = RailwayNetworkEditor.addControlPoint(network, "mid", "Mid", ControlPoint.Kind.STATION,
				new Zone("minecraft:overworld", List.of(new ZoneBox(point(4), point(6))), 1));

		assertTrue(PhysicalRoutePlanner.continueFromPosition(network,
				new TraversalRef("a_b", TrackDirection.FORWARD), "mid", 2).isEmpty());
	}

	@Test
	void partwayStartChargesOnlyRemainingDistanceOnFirstRail() throws Exception {
		RailwayNetwork network = baseNetwork();
		network = RailwayNetworkEditor.addTransition(network, "through_b_partway", "b",
				new TraversalRef("a_b", TrackDirection.FORWARD),
				new TraversalRef("b_c", TrackDirection.FORWARD), null);
		PhysicalRoutePlan full = PhysicalRoutePlanner.continueFrom(network,
				new TraversalRef("a_b", TrackDirection.FORWARD), "c").orElseThrow();
		PhysicalRoutePlan partway = PhysicalRoutePlanner.continueFromPosition(network,
				new TraversalRef("a_b", TrackDirection.FORWARD), "c", 2).orElseThrow();

		assertEquals(full.distance() - 8, partway.distance(), 0.0001);
		assertEquals(full.legs(), partway.legs());
	}
	@Test
	void onlyUsesExplicitlyEnabledTransitions() throws Exception {
		RailwayNetwork network = baseNetwork();
		assertTrue(PhysicalRoutePlanner.shortestPath(network, "a", "c").isEmpty());

		network = RailwayNetworkEditor.addTransition(network, "a_to_c", "b",
				new TraversalRef("a_b", TrackDirection.FORWARD),
				new TraversalRef("b_c", TrackDirection.FORWARD), "Välj knappen för C");
		PhysicalRoutePlan plan = PhysicalRoutePlanner.shortestPath(network, "a", "c").orElseThrow();

		assertEquals(List.of("a_b", "b_c"), plan.legs().stream().map(leg -> leg.track().id()).toList());
		assertEquals(20, plan.distance());
		assertEquals("Välj knappen för C",
				PhysicalInstructionResolver.between(network, plan.legs().get(0), plan.legs().get(1)).customText());
	}

	@Test
	void continuesFromTrackOccupiedBetweenControlPoints() throws Exception {
		RailwayNetwork network = baseNetwork();
		TraversalRef current = new TraversalRef("a_b", TrackDirection.FORWARD);
		assertTrue(PhysicalRoutePlanner.continueFrom(network, current, "c").isEmpty());

		network = RailwayNetworkEditor.addTransition(network, "through_b", "b", current,
				new TraversalRef("b_c", TrackDirection.FORWARD), null);
		PhysicalRoutePlan plan = PhysicalRoutePlanner.continueFrom(network, current, "c").orElseThrow();

		assertEquals(List.of("a_b", "b_c"), plan.legs().stream().map(leg -> leg.track().id()).toList());
	}

	private static RailwayNetwork baseNetwork() throws Exception {
		RailwayNetwork network = RailwayNetwork.EMPTY;
		network = RailwayNetworkEditor.addControlPoint(network, "a", "A", ControlPoint.Kind.STATION, zone(0));
		network = RailwayNetworkEditor.addControlPoint(network, "b", "B", ControlPoint.Kind.JUNCTION, zone(10));
		network = RailwayNetworkEditor.addControlPoint(network, "c", "C", ControlPoint.Kind.STATION, zone(20));
		network = RailwayNetworkEditor.addRecordedTrack(network, "a_b", "a", "b", "minecraft:overworld",
				List.of(point(0), point(10)));
		return RailwayNetworkEditor.addRecordedTrack(network, "b_c", "b", "c", "minecraft:overworld",
				List.of(point(10), point(20)));
	}

	private static Zone zone(int x) {
		return new Zone("minecraft:overworld", List.of(new ZoneBox(
				new TrackPoint(x - 1, 63, -1), new TrackPoint(x + 1, 66, 1))), 1);
	}

	private static TrackPoint point(int x) {
		return new TrackPoint(x, 64, 0);
	}
}
