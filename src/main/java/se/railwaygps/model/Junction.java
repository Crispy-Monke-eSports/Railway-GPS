package se.railwaygps.model;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

public record Junction(
		String id,
		String name,
		String dimension,
		TrackPoint position,
		double radius,
		Map<String, String> instructions
) {
	public Junction(String id, String name, String dimension, TrackPoint position, double radius) {
		this(id, name, dimension, position, radius, Map.of());
	}

	public Junction {
		instructions = instructions == null
				? Map.of()
				: Collections.unmodifiableMap(new TreeMap<>(instructions));
	}
}
