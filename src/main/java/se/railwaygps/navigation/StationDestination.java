package se.railwaygps.navigation;

import se.railwaygps.railway.model.ControlPoint;
import se.railwaygps.railway.model.RailwayNetwork;

/** Resolves player-selectable station IDs without substituting another destination. */
public final class StationDestination {
	private StationDestination() { }

	public static Status status(RailwayNetwork network, String id, String dimension) {
		ControlPoint point = network.controlPoints().get(id);
		if (point == null || point.kind() != ControlPoint.Kind.STATION) return Status.REMOVED;
		return point.zone().dimension().equals(dimension) ? Status.AVAILABLE : Status.WRONG_DIMENSION;
	}

	public enum Status { AVAILABLE, REMOVED, WRONG_DIMENSION }
}
