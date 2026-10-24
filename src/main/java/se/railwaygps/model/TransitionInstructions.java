package se.railwaygps.model;

import java.util.Map;
import java.util.Optional;

public final class TransitionInstructions {
	public static final String START = "*";
	private static final String SEPARATOR = ">";

	private TransitionInstructions() {
	}

	public static String key(String incomingRoute, String outgoingRoute) {
		return incomingRoute + SEPARATOR + outgoingRoute;
	}

	public static Optional<Transition> parse(String key) {
		if (key == null) {
			return Optional.empty();
		}
		int separator = key.indexOf(SEPARATOR);
		if (separator <= 0 || separator != key.lastIndexOf(SEPARATOR) || separator == key.length() - 1) {
			return Optional.empty();
		}
		return Optional.of(new Transition(key.substring(0, separator), key.substring(separator + 1)));
	}

	public static Optional<String> find(Map<String, String> instructions, String incomingRoute, String outgoingRoute) {
		String exact = instructions.get(key(incomingRoute, outgoingRoute));
		if (exact != null) {
			return Optional.of(exact);
		}
		return Optional.ofNullable(instructions.get(key(START, outgoingRoute)));
	}

	public record Transition(String incomingRoute, String outgoingRoute) {
	}
}
