package se.railwaygps.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TrackGeometryTest {
	@Test
	void simplifiesStraightRunsButPreservesBendsAndSlopes() {
		List<TrackPoint> points = List.of(
				new TrackPoint(0, 64, 0),
				new TrackPoint(1, 64, 0),
				new TrackPoint(2, 64, 0),
				new TrackPoint(2, 64, 1),
				new TrackPoint(2, 65, 2),
				new TrackPoint(2, 66, 3));

		assertEquals(List.of(
				new TrackPoint(0, 64, 0),
				new TrackPoint(2, 64, 0),
				new TrackPoint(2, 64, 1),
				new TrackPoint(2, 66, 3)), TrackGeometry.simplify(points));
	}

	@Test
	void removesDuplicateSamples() {
		assertEquals(List.of(new TrackPoint(0, 64, 0), new TrackPoint(1, 64, 0)), TrackGeometry.simplify(List.of(
				new TrackPoint(0, 64, 0),
				new TrackPoint(0, 64, 0),
				new TrackPoint(1, 64, 0))));
	}

	@Test
	void calculatesThreeDimensionalPolylineLength() {
		double length = TrackGeometry.length(List.of(
				new TrackPoint(0, 0, 0),
				new TrackPoint(3, 4, 0),
				new TrackPoint(3, 4, 12)));

		assertEquals(17.0, length);
	}
}
