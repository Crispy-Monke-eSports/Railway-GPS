package se.railwaygps.client.navigation;

import net.minecraft.network.chat.Component;

public record NavigationSnapshot(
		String destination,
		String nextNode,
		Component instruction,
		String railNames,
		double distanceToNext,
		boolean insideRedlight,
		double distanceRemaining,
		int junctionsRemaining,
		NavigationStatus status
) {
}
