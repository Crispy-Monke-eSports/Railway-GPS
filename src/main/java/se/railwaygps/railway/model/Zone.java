package se.railwaygps.railway.model;

import java.util.List;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public record Zone(String id, String dimension, List<ZoneBox> boxes, double exitMargin, int confirmationTicks) {
	public Zone(String dimension, List<ZoneBox> boxes, double exitMargin) {
		this(stableLegacyId(dimension, boxes), dimension, boxes, exitMargin, 3);
	}

	public Zone {
		if (id == null || !id.matches("[a-z0-9_-]+")) {
			throw new IllegalArgumentException("Zone id must use lowercase letters, numbers, '_' or '-'");
		}
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
		if (confirmationTicks < 1 || confirmationTicks > 200) {
			throw new IllegalArgumentException("Zone confirmation ticks must be between 1 and 200");
		}
	}

	public boolean contains(double x, double y, double z) {
		return boxes.stream().anyMatch(box -> box.contains(x, y, z, 0));
	}

	public boolean containsForExit(double x, double y, double z) {
		return boxes.stream().anyMatch(box -> box.contains(x, y, z, exitMargin));
	}

	private static String stableLegacyId(String dimension, List<ZoneBox> boxes) {
		String source = String.valueOf(dimension) + "|" + String.valueOf(boxes);
		return "zone-" + UUID.nameUUIDFromBytes(source.getBytes(StandardCharsets.UTF_8)).toString().replace("-", "");
	}
}
