package se.railwaygps.railway.model;

import java.util.List;

public record Zone(String dimension, List<ZoneBox> boxes, double exitMargin) {
	public Zone {
		if (dimension == null || dimension.isBlank()) {
			throw new IllegalArgumentException("Zone dimension is required");
		}
		boxes = boxes == null ? List.of() : List.copyOf(boxes);
		if (boxes.isEmpty()) {
			throw new IllegalArgumentException("Zone must contain at least one box");
		}
		if (!Double.isFinite(exitMargin) || exitMargin < 0) {
			throw new IllegalArgumentException("Zone exit margin must be finite and non-negative");
		}
	}

	public boolean contains(double x, double y, double z) {
		return boxes.stream().anyMatch(box -> box.contains(x, y, z, 0));
	}

	public boolean containsForExit(double x, double y, double z) {
		return boxes.stream().anyMatch(box -> box.contains(x, y, z, exitMargin));
	}
}
