package se.railwaygps.navigation;

import org.junit.jupiter.api.Test;
import se.railwaygps.model.TrackPoint;
import se.railwaygps.railway.model.PhysicalTrack;
import se.railwaygps.railway.model.RailwayNetwork;
import se.railwaygps.railway.model.TrackDirection;
import se.railwaygps.railway.model.TrackTransition;
import se.railwaygps.railway.model.TraversalRef;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RouteValidityTest {
	@Test void disabledIndividualTransitionInvalidatesOnlyThePlanThatUsesIt() {
		PhysicalTrack first = track("first", "a", "b");
		PhysicalTrack second = track("second", "b", "c");
		TraversalRef incoming = new TraversalRef("first", TrackDirection.FORWARD);
		TraversalRef outgoing = new TraversalRef("second", TrackDirection.FORWARD);
		PhysicalRoutePlan plan = new PhysicalRoutePlan("a", "c",
				List.of(new PhysicalRouteLeg(first, TrackDirection.FORWARD),
						new PhysicalRouteLeg(second, TrackDirection.FORWARD)), 20);
		RailwayNetwork enabled = new RailwayNetwork(1, Map.of(), Map.of(),
				Map.of(first.id(), first, second.id(), second),
				Map.of("passage", new TrackTransition("passage", "b", incoming, outgoing, true, null)), Map.of());
		RailwayNetwork disabled = new RailwayNetwork(2, Map.of(), Map.of(), enabled.tracks(),
				Map.of("passage", new TrackTransition("passage", "b", incoming, outgoing, false, null)), Map.of());

		assertTrue(RouteValidity.isUsable(enabled, plan));
		assertFalse(RouteValidity.isUsable(disabled, plan));
	}

	private static PhysicalTrack track(String id, String from, String to) {
		return new PhysicalTrack(id, "section", from, to, "minecraft:overworld",
				List.of(new TrackPoint(0, 64, 0), new TrackPoint(10, 64, 0)), 10,
				Set.of(TrackDirection.FORWARD), null, PhysicalTrack.Status.VERIFIED);
	}
}
