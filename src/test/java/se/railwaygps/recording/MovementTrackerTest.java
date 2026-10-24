package se.railwaygps.recording;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MovementTrackerTest {
	private static final double MAX_MOVEMENT_PER_TICK = 8.0;

	@Test
	void accumulatesBelievableMovementAsDistance() {
		MovementTracker tracker = new MovementTracker(0, 64, 0, MAX_MOVEMENT_PER_TICK);

		tracker.update(3, 64, 4);
		tracker.update(6, 64, 8);

		assertEquals(10.0, tracker.distance(), 0.000001);
		assertFalse(tracker.hasJumped());
	}

	@Test
	void acceptsMovementExactlyAtTheLimit() {
		MovementTracker tracker = new MovementTracker(0, 64, 0, MAX_MOVEMENT_PER_TICK);

		tracker.update(MAX_MOVEMENT_PER_TICK, 64, 0);

		assertEquals(MAX_MOVEMENT_PER_TICK, tracker.distance(), 0.000001);
		assertFalse(tracker.hasJumped());
	}

	@Test
	void flagsTeleportInsteadOfAddingItToTheDistance() {
		MovementTracker tracker = new MovementTracker(0, 64, 0, MAX_MOVEMENT_PER_TICK);

		tracker.update(120, 64, -300);

		assertEquals(0.0, tracker.distance(), 0.000001);
		assertTrue(tracker.hasJumped());
	}

	@Test
	void jumpFlagPersistsAcrossLaterContinuousMovement() {
		MovementTracker tracker = new MovementTracker(0, 64, 0, MAX_MOVEMENT_PER_TICK);

		tracker.update(1, 64, 0);
		tracker.update(500, 64, 0);
		tracker.update(502, 64, 0);

		assertEquals(3.0, tracker.distance(), 0.000001);
		assertTrue(tracker.hasJumped());
	}

	@Test
	void repeatedBelievableMovementNeverFlagsAJump() {
		MovementTracker tracker = new MovementTracker(0, 64, 0, MAX_MOVEMENT_PER_TICK);

		for (int tick = 1; tick <= 100; tick++) {
			tracker.update(tick * MAX_MOVEMENT_PER_TICK, 64, 0);
		}

		assertEquals(100 * MAX_MOVEMENT_PER_TICK, tracker.distance(), 0.000001);
		assertFalse(tracker.hasJumped());
	}
}
