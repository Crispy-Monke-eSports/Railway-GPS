package se.railwaygps.navigation;

import se.railwaygps.model.TrackPoint;

import java.util.List;
import java.util.Optional;

public final class PolylineMatcher {
	private PolylineMatcher() {
	}

	public static Optional<Match> nearest(List<TrackPoint> points, double x, double y, double z) {
		if (points.size() < 2) {
			return Optional.empty();
		}
		double bestDistanceSquared = Double.POSITIVE_INFINITY;
		double bestAlong = 0;
		double traversed = 0;
		double total = polylineLength(points);

		for (int index = 1; index < points.size(); index++) {
			TrackPoint first = points.get(index - 1);
			TrackPoint second = points.get(index);
			double ax = centerX(first);
			double ay = centerY(first);
			double az = centerZ(first);
			double dx = centerX(second) - ax;
			double dy = centerY(second) - ay;
			double dz = centerZ(second) - az;
			double segmentSquared = dx * dx + dy * dy + dz * dz;
			double segmentLength = Math.sqrt(segmentSquared);
			if (segmentSquared == 0) {
				continue;
			}
			double t = ((x - ax) * dx + (y - ay) * dy + (z - az) * dz) / segmentSquared;
			t = Math.max(0, Math.min(1, t));
			double px = ax + t * dx;
			double py = ay + t * dy;
			double pz = az + t * dz;
			double distanceSquared = square(x - px) + square(y - py) + square(z - pz);
			if (distanceSquared < bestDistanceSquared) {
				bestDistanceSquared = distanceSquared;
				bestAlong = traversed + t * segmentLength;
			}
			traversed += segmentLength;
		}
		return Optional.of(new Match(Math.sqrt(bestDistanceSquared), bestAlong, total));
	}

	private static double polylineLength(List<TrackPoint> points) {
		double result = 0;
		for (int index = 1; index < points.size(); index++) {
			TrackPoint first = points.get(index - 1);
			TrackPoint second = points.get(index);
			result += Math.sqrt(
					square(centerX(second) - centerX(first))
							+ square(centerY(second) - centerY(first))
							+ square(centerZ(second) - centerZ(first)));
		}
		return result;
	}

	private static double centerX(TrackPoint point) {
		return point.x() + 0.5;
	}

	private static double centerY(TrackPoint point) {
		return point.y() + 0.5;
	}

	private static double centerZ(TrackPoint point) {
		return point.z() + 0.5;
	}

	private static double square(double value) {
		return value * value;
	}

	public record Match(double distanceToTrack, double distanceAlong, double trackLength) {
	}
}
