package se.railwaygps.navigation;

import org.junit.jupiter.api.Test;
import se.railwaygps.model.TrackPoint;
import se.railwaygps.railway.model.PhysicalTrack;
import se.railwaygps.railway.model.TrackDirection;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RouteChangeTest {
	@Test void onlyReportsAnActuallyDifferentDirectedRoute() {
		PhysicalRoutePlan first = plan("destination", "track-a", TrackDirection.FORWARD);
		assertFalse(RouteChange.replaced(first, plan("destination", "track-a", TrackDirection.FORWARD)));
		assertTrue(RouteChange.replaced(first, plan("destination", "track-b", TrackDirection.FORWARD)));
		assertTrue(RouteChange.replaced(first, plan("destination", "track-a", TrackDirection.REVERSE)));
		assertFalse(RouteChange.replaced(null, first));
	}

	private static PhysicalRoutePlan plan(String destination, String id, TrackDirection direction) {
		PhysicalTrack track = new PhysicalTrack(id, "section", "a", "b", "minecraft:overworld",
				List.of(new TrackPoint(0, 64, 0), new TrackPoint(10, 64, 0)), 10,
				Set.of(direction), null, PhysicalTrack.Status.VERIFIED);
		return new PhysicalRoutePlan("a", destination, List.of(new PhysicalRouteLeg(track, direction)), 10);
	}
}
