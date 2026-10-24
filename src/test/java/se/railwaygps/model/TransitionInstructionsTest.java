package se.railwaygps.model;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransitionInstructionsTest {
	@Test
	void exactInstructionWinsOverStartFallback() {
		Map<String, String> instructions = Map.of(
				"*>out", "Fallback",
				"in>out", "Exact");

		assertEquals("Exact", TransitionInstructions.find(instructions, "in", "out").orElseThrow());
		assertEquals("Fallback", TransitionInstructions.find(instructions, "other", "out").orElseThrow());
	}

	@Test
	void parsesOnlyCompleteTransitionKeys() {
		TransitionInstructions.Transition transition = TransitionInstructions.parse("incoming>outgoing").orElseThrow();
		assertEquals("incoming", transition.incomingRoute());
		assertEquals("outgoing", transition.outgoingRoute());
		assertTrue(TransitionInstructions.parse("missing_separator").isEmpty());
		assertTrue(TransitionInstructions.parse("too>many>parts").isEmpty());
	}
}
