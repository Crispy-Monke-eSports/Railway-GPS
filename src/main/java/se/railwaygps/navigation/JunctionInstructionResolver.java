package se.railwaygps.navigation;

import se.railwaygps.model.RailwayNetwork;
import se.railwaygps.model.TrackPoint;
import se.railwaygps.model.TransitionInstructions;

import java.util.List;
import java.util.Optional;

public final class JunctionInstructionResolver {
	private static final double SAMPLE_DISTANCE = 6.0;
	private static final double STRAIGHT_COSINE = Math.cos(Math.toRadians(35));

	private JunctionInstructionResolver() {
	}

	public static Instruction between(RailwayNetwork network, RouteLeg incoming, RouteLeg outgoing) {
		if (!incoming.to().equals(outgoing.from())) {
			throw new IllegalArgumentException("Route legs do not meet at the same node");
		}
		Optional<String> custom = TransitionInstructions.find(
				network.nodeInstructions(incoming.to()), incoming.route().id(), outgoing.route().id());
		if (custom.isPresent()) {
			return new Instruction(Direction.CUSTOM, custom.get());
		}

		Vector approach = arrivalVector(orientedPoints(incoming));
		Vector departure = departureVector(orientedPoints(outgoing));
		return classify(approach, departure);
	}

	public static Instruction fromStart(RailwayNetwork network, RouteLeg outgoing, double headingX, double headingZ) {
		Optional<String> custom = TransitionInstructions.find(
				network.nodeInstructions(outgoing.from()), TransitionInstructions.START, outgoing.route().id());
		if (custom.isPresent()) {
			return new Instruction(Direction.CUSTOM, custom.get());
		}
		return classify(normalize(headingX, headingZ), departureVector(orientedPoints(outgoing)));
	}

	private static Instruction classify(Vector approach, Vector departure) {
		if (approach == null || departure == null) {
			return new Instruction(Direction.UNKNOWN, null);
		}
		double dot = approach.x() * departure.x() + approach.z() * departure.z();
		if (dot >= STRAIGHT_COSINE) {
			return new Instruction(Direction.STRAIGHT, null);
		}
		if (dot <= -STRAIGHT_COSINE) {
			return new Instruction(Direction.BACK, null);
		}
		double cross = approach.x() * departure.z() - approach.z() * departure.x();
		return new Instruction(cross > 0 ? Direction.RIGHT : Direction.LEFT, null);
	}

	private static List<TrackPoint> orientedPoints(RouteLeg leg) {
		return leg.reversed() ? leg.route().points().reversed() : leg.route().points();
	}

	private static Vector arrivalVector(List<TrackPoint> points) {
		if (points.size() < 2) {
			return null;
		}
		TrackPoint end = points.getLast();
		for (int index = points.size() - 2; index >= 0; index--) {
			TrackPoint point = points.get(index);
			if (horizontalDistance(point, end) >= SAMPLE_DISTANCE || index == 0) {
				return normalize(end.x() - point.x(), end.z() - point.z());
			}
		}
		return null;
	}

	private static Vector departureVector(List<TrackPoint> points) {
		if (points.size() < 2) {
			return null;
		}
		TrackPoint start = points.getFirst();
		for (int index = 1; index < points.size(); index++) {
			TrackPoint point = points.get(index);
			if (horizontalDistance(start, point) >= SAMPLE_DISTANCE || index == points.size() - 1) {
				return normalize(point.x() - start.x(), point.z() - start.z());
			}
		}
		return null;
	}

	private static double horizontalDistance(TrackPoint first, TrackPoint second) {
		return Math.hypot(second.x() - first.x(), second.z() - first.z());
	}

	private static Vector normalize(double x, double z) {
		double length = Math.hypot(x, z);
		return length < 0.001 ? null : new Vector(x / length, z / length);
	}

	public enum Direction {
		STRAIGHT,
		LEFT,
		RIGHT,
		BACK,
		CUSTOM,
		UNKNOWN
	}

	public record Instruction(Direction direction, String customText) {
	}

	private record Vector(double x, double z) {
	}
}
