package se.railwaygps.navigation;

import java.util.List;

public record RoutePlan(String start, String destination, List<RouteLeg> legs, double distance) {
	public RoutePlan {
		legs = List.copyOf(legs);
	}
}
