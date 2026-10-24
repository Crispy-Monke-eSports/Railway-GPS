package se.railwaygps.model;

import java.util.List;

public record Route(
		String id,
		String name,
		String from,
		String to,
		String dimension,
		boolean bidirectional,
		double distance,
		List<TrackPoint> points
) {
	public Route {
		points = points == null ? null : List.copyOf(points);
	}
}
