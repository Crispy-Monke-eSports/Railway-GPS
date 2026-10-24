package se.railwaygps.model;

import java.util.ArrayList;
import java.util.List;

public final class TrackGeometry {
	private TrackGeometry() {
	}

	/** Removes duplicates and interior points on a straight line while preserving bends and slopes. */
	public static List<TrackPoint> simplify(List<TrackPoint> input) {
		List<TrackPoint> unique = new ArrayList<>();
		for (TrackPoint point : input) {
			if (unique.isEmpty() || !unique.getLast().equals(point)) {
				unique.add(point);
			}
		}
		if (unique.size() < 3) {
			return List.copyOf(unique);
		}

		List<TrackPoint> result = new ArrayList<>();
		result.add(unique.getFirst());
		for (int index = 1; index < unique.size() - 1; index++) {
			TrackPoint previous = result.getLast();
			TrackPoint current = unique.get(index);
			TrackPoint next = unique.get(index + 1);
			if (!isStraightContinuation(previous, current, next)) {
				result.add(current);
			}
		}
		result.add(unique.getLast());
		return List.copyOf(result);
	}

	public static double length(List<TrackPoint> points) {
		double length = 0;
		for (int index = 1; index < points.size(); index++) {
			TrackPoint first = points.get(index - 1);
			TrackPoint second = points.get(index);
			double dx = second.x() - first.x();
			double dy = second.y() - first.y();
			double dz = second.z() - first.z();
			length += Math.sqrt(dx * dx + dy * dy + dz * dz);
		}
		return length;
	}

	private static boolean isStraightContinuation(TrackPoint first, TrackPoint middle, TrackPoint last) {
		long ax = middle.x() - first.x();
		long ay = middle.y() - first.y();
		long az = middle.z() - first.z();
		long bx = last.x() - middle.x();
		long by = last.y() - middle.y();
		long bz = last.z() - middle.z();

		long crossX = ay * bz - az * by;
		long crossY = az * bx - ax * bz;
		long crossZ = ax * by - ay * bx;
		long dot = ax * bx + ay * by + az * bz;
		return crossX == 0 && crossY == 0 && crossZ == 0 && dot > 0;
	}
}
