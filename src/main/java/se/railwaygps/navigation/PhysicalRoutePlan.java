package se.railwaygps.navigation;

import java.util.List;

public record PhysicalRoutePlan(String start, String destination, List<PhysicalRouteLeg> legs, double distance) {
	public PhysicalRoutePlan {
		legs = List.copyOf(legs);
	}
}
