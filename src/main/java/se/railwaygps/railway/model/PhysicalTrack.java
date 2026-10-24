package se.railwaygps.railway.model;

import se.railwaygps.model.TrackPoint;

import java.util.List;
import java.util.Set;

public record PhysicalTrack(
		String id,
		String sectionId,
		String fromControlPoint,
		String toControlPoint,
		String dimension,
		List<TrackPoint> points,
		double length,
		Set<TrackDirection> directions,
		String pairedTrackId,
		Status status
) {
	public PhysicalTrack {
		points = points == null ? List.of() : List.copyOf(points);
		directions = directions == null ? Set.of() : Set.copyOf(directions);
		status = status == null ? Status.DRAFT : status;
	}

	public enum Status {
		DRAFT,
		VERIFIED,
		DISABLED
	}
}
