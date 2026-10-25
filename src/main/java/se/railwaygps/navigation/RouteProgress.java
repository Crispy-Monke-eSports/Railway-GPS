package se.railwaygps.navigation;

import se.railwaygps.railway.model.ControlPoint;
import se.railwaygps.railway.model.RailwayNetwork;

import java.util.OptionalDouble;

public final class RouteProgress {
	private RouteProgress() { }

	public static OptionalDouble distanceToNextRedlight(RailwayNetwork network, PhysicalRoutePlan plan,
			int currentLeg, double remainingOnCurrentLeg) {
		double distance = remainingOnCurrentLeg;
		for (int index = currentLeg; index < plan.legs().size(); index++) {
			PhysicalRouteLeg leg = plan.legs().get(index);
			ControlPoint endpoint = network.controlPoints().get(leg.to());
			if (endpoint != null && endpoint.isRedlight()) return OptionalDouble.of(distance);
			if (index + 1 < plan.legs().size()) distance += plan.legs().get(index + 1).track().length();
		}
		return OptionalDouble.empty();
	}
}
