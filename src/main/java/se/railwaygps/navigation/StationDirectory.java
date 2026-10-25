package se.railwaygps.navigation;

import se.railwaygps.railway.model.ControlPoint;
import se.railwaygps.railway.model.RailwayNetwork;

import java.util.Comparator;
import java.util.List;

/** Player-facing station destinations, filtered by dimension and sorted by display name. */
public final class StationDirectory {
	private StationDirectory() { }

	public static List<ControlPoint> inDimension(RailwayNetwork network, String dimension) {
		return network.controlPoints().values().stream()
				.filter(point -> point.kind() == ControlPoint.Kind.STATION)
				.filter(point -> point.zone().dimension().equals(dimension))
				.sorted(Comparator.comparing(ControlPoint::name, String.CASE_INSENSITIVE_ORDER)
						.thenComparing(ControlPoint::id))
				.toList();
	}
}
