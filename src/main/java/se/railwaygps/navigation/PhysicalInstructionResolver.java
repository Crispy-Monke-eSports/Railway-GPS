package se.railwaygps.navigation;

import se.railwaygps.model.TrackPoint;
import se.railwaygps.railway.model.RailwayNetwork;
import se.railwaygps.railway.model.TrackTransition;

import java.util.List;

public final class PhysicalInstructionResolver {
	private static final double SAMPLE_DISTANCE = 6.0;
	private static final double STRAIGHT_COSINE = Math.cos(Math.toRadians(35));

	private PhysicalInstructionResolver() {
	}

	public static Instruction between(
			RailwayNetwork network, PhysicalRouteLeg incoming, PhysicalRouteLeg outgoing) {
		if (!incoming.to().equals(outgoing.from())) {
			throw new IllegalArgumentException("Route legs do not meet at the same control point");
		}
		String custom = network.transitions().values().stream()
				.filter(TrackTransition::enabled)
				.filter(transition -> transition.controlPointId().equals(incoming.to()))
				.filter(transition -> transition.incoming().equals(incoming.traversal()))
				.filter(transition -> transition.outgoing().equals(outgoing.traversal()))
				.map(TrackTransition::instruction).filter(text -> text != null && !text.isBlank())
				.findFirst().orElse(null);
		if (custom != null) return new Instruction(Direction.CUSTOM, custom);
		return classify(arrivalVector(incoming.points()), departureVector(outgoing.points()));
	}

	public static Instruction fromStart(PhysicalRouteLeg outgoing, double headingX, double headingZ) {
		return classify(normalize(headingX, headingZ), departureVector(outgoing.points()));
	}

	private static Instruction classify(Vector approach, Vector departure) {
		if (approach == null || departure == null) return new Instruction(Direction.UNKNOWN, null);
		double dot = approach.x() * departure.x() + approach.z() * departure.z();
		if (dot >= STRAIGHT_COSINE) return new Instruction(Direction.STRAIGHT, null);
		if (dot <= -STRAIGHT_COSINE) return new Instruction(Direction.BACK, null);
		double cross = approach.x() * departure.z() - approach.z() * departure.x();
		return new Instruction(cross > 0 ? Direction.RIGHT : Direction.LEFT, null);
	}

	private static Vector arrivalVector(List<TrackPoint> points) {
		if (points.size() < 2) return null;
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
		if (points.size() < 2) return null;
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

	public enum Direction { STRAIGHT, LEFT, RIGHT, BACK, CUSTOM, UNKNOWN }
	public record Instruction(Direction direction, String customText) { }
	private record Vector(double x, double z) { }
}
