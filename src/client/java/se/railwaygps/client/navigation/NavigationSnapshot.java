package se.railwaygps.client.navigation;

public record NavigationSnapshot(
		String destination,
		String nextNode,
		String instruction,
		double distanceToNext,
		double distanceRemaining,
		int junctionsRemaining,
		NavigationStatus status
) {
}
