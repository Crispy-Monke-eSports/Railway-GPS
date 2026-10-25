package se.railwaygps.navigation;

import java.util.List;

/** Compares route identity by directed physical passages, independent of refreshed model instances. */
public final class RouteChange {
	private RouteChange() { }

	public static boolean replaced(PhysicalRoutePlan previous, PhysicalRoutePlan next) {
		if (previous == null || next == null) return false;
		List<?> before = previous.legs().stream().map(PhysicalRouteLeg::traversal).toList();
		List<?> after = next.legs().stream().map(PhysicalRouteLeg::traversal).toList();
		return !previous.destination().equals(next.destination()) || !before.equals(after);
	}
}
