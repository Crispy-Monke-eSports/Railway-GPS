package se.railwaygps.client;

import se.railwaygps.network.TransitionToggleResultPayload;

public final class ClientTransitionToggleResult {
	private static TransitionToggleResultPayload result;
	private static long revision;
	private ClientTransitionToggleResult() { }
	public static TransitionToggleResultPayload get() { return result; }
	public static long revision() { return revision; }
	public static void replace(TransitionToggleResultPayload next) { result = next; revision++; }
	public static void clear() { result = null; revision++; }
}
