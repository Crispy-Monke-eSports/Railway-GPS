package se.railwaygps.model;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

public record Station(
		String id,
		String name,
		String dimension,
		TrackPoint position,
		Map<String, String> instructions
) {
	public Station(String id, String name, String dimension, TrackPoint position) {
		this(id, name, dimension, position, Map.of());
	}

	public Station {
		instructions = instructions == null
				? Map.of()
				: Collections.unmodifiableMap(new TreeMap<>(instructions));
	}
}
