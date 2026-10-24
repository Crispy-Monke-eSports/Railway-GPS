package se.railwaygps.railway.model;

import java.util.Map;

public record RailwayNetwork(
		long revision,
		Map<String, ControlPoint> controlPoints,
		Map<String, TrackSection> sections,
		Map<String, PhysicalTrack> tracks,
		Map<String, TrackTransition> transitions,
		Map<String, TrackLabel> labels
) {
	public static final RailwayNetwork EMPTY = new RailwayNetwork(0, Map.of(), Map.of(), Map.of(), Map.of(), Map.of());

	public RailwayNetwork {
		controlPoints = controlPoints == null ? Map.of() : Map.copyOf(controlPoints);
		sections = sections == null ? Map.of() : Map.copyOf(sections);
		tracks = tracks == null ? Map.of() : Map.copyOf(tracks);
		transitions = transitions == null ? Map.of() : Map.copyOf(transitions);
		labels = labels == null ? Map.of() : Map.copyOf(labels);
	}
}
