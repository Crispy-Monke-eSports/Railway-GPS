package se.railwaygps.railway.model;

import java.util.Map;
import java.util.Set;

public record RailwayNetwork(
		long revision,
		Map<String, ControlPoint> controlPoints,
		Map<String, TrackSection> sections,
		Map<String, PhysicalTrack> tracks,
		Map<String, TrackTransition> transitions,
		Map<String, TrackLabel> labels,
		Set<String> rejectedTrackPairings
) {
	public static final RailwayNetwork EMPTY = new RailwayNetwork(0, Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), Set.of());

	public RailwayNetwork(long revision, Map<String, ControlPoint> controlPoints, Map<String, TrackSection> sections,
			Map<String, PhysicalTrack> tracks, Map<String, TrackTransition> transitions, Map<String, TrackLabel> labels) {
		this(revision, controlPoints, sections, tracks, transitions, labels, Set.of());
	}

	public RailwayNetwork {
		controlPoints = controlPoints == null ? Map.of() : Map.copyOf(controlPoints);
		sections = sections == null ? Map.of() : Map.copyOf(sections);
		tracks = tracks == null ? Map.of() : Map.copyOf(tracks);
		transitions = transitions == null ? Map.of() : Map.copyOf(transitions);
		labels = labels == null ? Map.of() : Map.copyOf(labels);
		rejectedTrackPairings = rejectedTrackPairings == null ? Set.of() : Set.copyOf(rejectedTrackPairings);
	}
}
