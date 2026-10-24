package se.railwaygps.v2.model;

import se.railwaygps.model.TrackPoint;

public record ZoneBox(TrackPoint min, TrackPoint max) {
	public ZoneBox {
		if (min == null || max == null) {
			throw new IllegalArgumentException("Zone box corners are required");
		}
		if (min.x() > max.x() || min.y() > max.y() || min.z() > max.z()) {
			throw new IllegalArgumentException("Zone box minimum must not exceed maximum");
		}
	}

	public boolean contains(double x, double y, double z, double margin) {
		return x >= min.x() - margin && x <= max.x() + 1 + margin
				&& y >= min.y() - margin && y <= max.y() + 1 + margin
				&& z >= min.z() - margin && z <= max.z() + 1 + margin;
	}
}
