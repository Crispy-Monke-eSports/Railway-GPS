package se.railwaygps.navigation;

import org.junit.jupiter.api.Test;
import se.railwaygps.model.TrackPoint;
import se.railwaygps.railway.model.PhysicalTrack;
import se.railwaygps.railway.model.TrackDirection;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ActiveRouteTracksTest {
	@Test void exposesOnlyCurrentAndRemainingDirectedTracks() {
		PhysicalRoutePlan plan = new PhysicalRoutePlan("a", "d", List.of(
				leg("past", TrackDirection.FORWARD), leg("current", TrackDirection.REVERSE),
				leg("future", TrackDirection.FORWARD)), 30);
		assertEquals(Set.of("current", "future"), ActiveRouteTracks.remaining(Optional.of(plan), 1).keySet());
		assertEquals(TrackDirection.REVERSE, ActiveRouteTracks.remaining(Optional.of(plan), 1).get("current"));
		assertEquals(0, ActiveRouteTracks.remaining(Optional.empty(), 0).size());
	}

	private static PhysicalRouteLeg leg(String id, TrackDirection direction) {
		PhysicalTrack track = new PhysicalTrack(id, "section", "a", "b", "minecraft:overworld",
				List.of(new TrackPoint(0, 64, 0), new TrackPoint(10, 64, 0)), 10,
				Set.of(direction), null, PhysicalTrack.Status.VERIFIED);
		return new PhysicalRouteLeg(track, direction);
	}
}
