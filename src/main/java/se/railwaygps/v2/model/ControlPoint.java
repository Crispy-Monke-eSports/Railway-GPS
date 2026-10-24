package se.railwaygps.v2.model;

import java.util.Map;

public record ControlPoint(
		String id,
		String name,
		Kind kind,
		Zone zone,
		Map<String, Platform> platforms
) {
	public ControlPoint {
		platforms = platforms == null ? Map.of() : Map.copyOf(platforms);
	}

	public enum Kind {
		STATION,
		JUNCTION,
		TERMINAL,
		DIMENSION_GATE
	}

	public record Platform(String id, String name, Zone zone) {
	}
}
