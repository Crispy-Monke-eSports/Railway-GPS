package se.railwaygps.navigation;

import se.railwaygps.railway.model.PhysicalTrack;
import se.railwaygps.railway.model.RailwayNetwork;

/** Checks whether every directed passage and inter-track transition in an active plan remains usable. */
public final class RouteValidity {
	private RouteValidity() { }

	public static boolean isUsable(RailwayNetwork network, PhysicalRoutePlan plan) {
		for (PhysicalRouteLeg leg : plan.legs()) {
			PhysicalTrack track = network.tracks().get(leg.track().id());
			if (track == null || track.status() != PhysicalTrack.Status.VERIFIED
					|| !track.directions().contains(leg.direction())) return false;
		}
		for (int index = 1; index < plan.legs().size(); index++) {
			PhysicalRouteLeg incoming = plan.legs().get(index - 1);
			PhysicalRouteLeg outgoing = plan.legs().get(index);
			boolean enabled = network.transitions().values().stream().anyMatch(transition -> transition.enabled()
					&& transition.incoming().equals(incoming.traversal())
					&& transition.outgoing().equals(outgoing.traversal()));
			if (!enabled) return false;
		}
		return true;
	}
}
