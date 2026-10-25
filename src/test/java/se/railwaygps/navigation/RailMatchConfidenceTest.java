package se.railwaygps.navigation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RailMatchConfidenceTest {
	@Test void rejectsCloseParallelRails() {
		assertTrue(RailMatchConfidence.uniqueTrack(List.of(
				new RailMatchConfidence.Candidate("east", 0.10, true),
				new RailMatchConfidence.Candidate("west", 0.18, true)), 2, 0.15).isEmpty());
	}

	@Test void rejectsMovementAgainstTheOnlyVerifiedDirection() {
		assertTrue(RailMatchConfidence.uniqueTrack(List.of(
				new RailMatchConfidence.Candidate("east", 0.10, false)), 2, 0.15).isEmpty());
	}

	@Test void acceptsOneClearlyNearestAlignedRail() {
		assertEquals("east", RailMatchConfidence.uniqueTrack(List.of(
				new RailMatchConfidence.Candidate("east", 0.10, true),
				new RailMatchConfidence.Candidate("west", 0.40, true)), 2, 0.15).orElseThrow());
	}
}
