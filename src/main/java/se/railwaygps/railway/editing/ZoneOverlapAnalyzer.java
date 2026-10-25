package se.railwaygps.railway.editing;

import se.railwaygps.railway.model.ControlPoint;
import se.railwaygps.railway.model.RailwayNetwork;
import se.railwaygps.railway.model.ZoneBox;

import java.util.ArrayList;
import java.util.List;

public final class ZoneOverlapAnalyzer {
	private ZoneOverlapAnalyzer() { }

	public static List<Overlap> find(RailwayNetwork network) {
		List<ControlPoint> points = new ArrayList<>(network.controlPoints().values());
		List<Overlap> result = new ArrayList<>();
		for (int first = 0; first < points.size(); first++) {
			for (int second = first + 1; second < points.size(); second++) {
				ControlPoint a = points.get(first);
				ControlPoint b = points.get(second);
				if (a == null || b == null || a.zone() == null || b.zone() == null) continue;
				if (!a.zone().dimension().equals(b.zone().dimension())) continue;
				if (a.zone().boxes().stream().anyMatch(one -> b.zone().boxes().stream()
						.anyMatch(two -> intersects(one, two)))) {
					result.add(new Overlap(a.id(), b.id(), key(a.zone().id(), b.zone().id())));
				}
			}
		}
		return List.copyOf(result);
	}

	public static String key(String firstZoneId, String secondZoneId) {
		return firstZoneId.compareTo(secondZoneId) <= 0 ? firstZoneId + "|" + secondZoneId
				: secondZoneId + "|" + firstZoneId;
	}

	private static boolean intersects(ZoneBox first, ZoneBox second) {
		return first.min().x() <= second.max().x() && first.max().x() >= second.min().x()
				&& first.min().y() <= second.max().y() && first.max().y() >= second.min().y()
				&& first.min().z() <= second.max().z() && first.max().z() >= second.min().z();
	}

	public record Overlap(String firstControlPointId, String secondControlPointId, String key) { }
}
