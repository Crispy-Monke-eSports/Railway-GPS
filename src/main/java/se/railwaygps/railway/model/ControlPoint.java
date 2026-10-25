package se.railwaygps.railway.model;

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
		REDLIGHT,
		/** Legacy serialized name retained for schema-2 compatibility. */
		JUNCTION,
		TERMINAL,
		DIMENSION_GATE
	}

	public boolean isRedlight() {
		return kind == Kind.REDLIGHT || kind == Kind.JUNCTION;
	}

	public record Platform(String id, String name, Zone zone) {
	}
}
