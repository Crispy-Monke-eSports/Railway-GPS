package se.railwaygps.railway.model;

import org.junit.jupiter.api.Test;
import se.railwaygps.model.TrackPoint;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZoneOccupancyTrackerTest {
	private final Zone zone = new Zone("zone-test", "minecraft:overworld", List.of(
			new ZoneBox(new TrackPoint(0, 60, 0), new TrackPoint(2, 64, 2)),
			new ZoneBox(new TrackPoint(5, 60, 0), new TrackPoint(7, 64, 2))), 1, 3);

	@Test
	void requiresStableEntryAndExitWhileMarginSuppressesJitter() {
		ZoneOccupancyTracker tracker = new ZoneOccupancyTracker();
		assertEquals(ZoneOccupancyTracker.Event.NONE, tracker.observe(zone, 1, 62, 1));
		assertEquals(ZoneOccupancyTracker.Event.NONE, tracker.observe(zone, 1, 62, 1));
		assertEquals(ZoneOccupancyTracker.Event.ENTERED, tracker.observe(zone, 1, 62, 1));
		assertTrue(tracker.inside());

		assertEquals(ZoneOccupancyTracker.Event.NONE, tracker.observe(zone, 3.5, 62, 1));
		assertEquals(ZoneOccupancyTracker.Event.NONE, tracker.observe(zone, 20, 62, 1));
		assertEquals(ZoneOccupancyTracker.Event.NONE, tracker.observe(zone, 2, 62, 1));
		assertEquals(ZoneOccupancyTracker.Event.NONE, tracker.observe(zone, 20, 62, 1));
		assertEquals(ZoneOccupancyTracker.Event.NONE, tracker.observe(zone, 20, 62, 1));
		assertEquals(ZoneOccupancyTracker.Event.EXITED, tracker.observe(zone, 20, 62, 1));
		assertFalse(tracker.inside());
	}

	@Test
	void multiBoxMembershipPreservesVerticalSeparation() {
		assertTrue(zone.contains(6, 62, 1));
		assertFalse(zone.contains(3.5, 62, 1));
		assertFalse(zone.contains(1, 80, 1));
	}
}
