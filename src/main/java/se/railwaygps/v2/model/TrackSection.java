package se.railwaygps.v2.model;

import java.util.Set;

public record TrackSection(
		String id,
		String endpointA,
		String endpointB,
		Set<String> trackIds
) {
	public TrackSection {
		trackIds = trackIds == null ? Set.of() : Set.copyOf(trackIds);
	}
}
