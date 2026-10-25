package se.railwaygps.navigation;

import org.junit.jupiter.api.Test;
import se.railwaygps.model.TrackPoint;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PolylineMatcherTest {
	@Test
	void projectsPositionOntoNearestSegment() {
		PolylineMatcher.Match match = PolylineMatcher.nearest(
				List.of(new TrackPoint(0, 0, 0), new TrackPoint(10, 0, 0), new TrackPoint(10, 0, 10)),
				8.5, 0.5, 2.5).orElseThrow();

		assertEquals(2.0, match.distanceToTrack(), 0.001);
		assertEquals(8.0, match.distanceAlong(), 0.001);
		assertEquals(20.0, match.trackLength(), 0.001);
		assertEquals(1.0, match.movementAlignment(2, 0, 0), 0.001);
		assertEquals(-1.0, match.movementAlignment(-2, 0, 0), 0.001);
	}

	@Test
	void clampsProjectionToPolylineEnds() {
		PolylineMatcher.Match match = PolylineMatcher.nearest(
				List.of(new TrackPoint(0, 0, 0), new TrackPoint(10, 0, 0)),
				-2.5, 0.5, 0.5).orElseThrow();

		assertEquals(3.0, match.distanceToTrack(), 0.001);
		assertEquals(0.0, match.distanceAlong(), 0.001);
	}

	@Test
	void stationaryMovementNeverClaimsATrackDirection() {
		PolylineMatcher.Match match = PolylineMatcher.nearest(
				List.of(new TrackPoint(0, 0, 0), new TrackPoint(10, 0, 0)),
				5.5, 0.5, 0.5).orElseThrow();
		assertEquals(0, match.movementAlignment(0, 0, 0), 0.001);
	}
}
