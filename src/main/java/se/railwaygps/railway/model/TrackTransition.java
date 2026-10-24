package se.railwaygps.railway.model;

public record TrackTransition(
		String id,
		String controlPointId,
		TraversalRef incoming,
		TraversalRef outgoing,
		boolean enabled,
		String instruction
) {
}
