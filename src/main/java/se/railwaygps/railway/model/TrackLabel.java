package se.railwaygps.railway.model;

import java.util.List;

public record TrackLabel(
		String id,
		String name,
		Type type,
		int priority,
		Integer color,
		List<TrackRange> ranges
) {
	public TrackLabel {
		ranges = ranges == null ? List.of() : List.copyOf(ranges);
	}

	public enum Type {
		WAY,
		BRIDGE,
		TUNNEL,
		DISTRICT,
		CUSTOM
	}
}
