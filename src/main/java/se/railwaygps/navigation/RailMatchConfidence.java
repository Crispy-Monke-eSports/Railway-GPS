package se.railwaygps.navigation;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Rejects spatially indistinguishable physical rails instead of guessing one by iteration order. */
public final class RailMatchConfidence {
	private RailMatchConfidence() { }

	public static Optional<String> uniqueTrack(List<Candidate> candidates, double maximumDistance, double ambiguityMargin) {
		List<Candidate> sorted = candidates.stream().filter(candidate -> candidate.directionAligned())
				.sorted(Comparator.comparingDouble(Candidate::distance)).toList();
		if (sorted.isEmpty() || sorted.getFirst().distance() > maximumDistance) return Optional.empty();
		double limit = sorted.getFirst().distance() + ambiguityMargin;
		List<String> plausible = sorted.stream().filter(candidate -> candidate.distance() <= limit)
				.map(Candidate::trackId).distinct().toList();
		return plausible.size() == 1 ? Optional.of(plausible.getFirst()) : Optional.empty();
	}

	public record Candidate(String trackId, double distance, boolean directionAligned) { }
}
