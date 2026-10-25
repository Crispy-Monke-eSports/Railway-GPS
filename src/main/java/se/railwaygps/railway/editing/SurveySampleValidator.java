package se.railwaygps.railway.editing;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.RailShape;
import se.railwaygps.model.TrackPoint;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/** Validates the raw, tick-by-tick physical rail path before simplification or publication. */
public final class SurveySampleValidator {
	private static final int MAX_MISSING_RAIL_TICKS = 2;
	private final List<TrackPoint> points = new ArrayList<>();
	private RailShape previousShape;
	private int missingRailTicks;
	private Failure failure;

	public void observeRail(TrackPoint point, RailShape shape) {
		if (failure != null) return;
		missingRailTicks = 0;
		if (points.isEmpty()) {
			points.add(point);
			previousShape = shape;
			return;
		}
		TrackPoint previous = points.getLast();
		if (previous.equals(point)) {
			previousShape = shape;
			return;
		}
		if (!connected(previous, previousShape, point, shape)) {
			failure = new Failure(Reason.DISCONNECTED_RAILS, previous, point);
			return;
		}
		points.add(point);
		previousShape = shape;
	}

	public void observeMissingRail() {
		if (failure != null) return;
		missingRailTicks++;
		if (missingRailTicks > MAX_MISSING_RAIL_TICKS) {
			failure = new Failure(Reason.RAIL_LOST, points.isEmpty() ? null : points.getLast(), null);
		}
	}

	public void observeTeleport() {
		if (failure == null) failure = new Failure(Reason.POSITION_JUMP,
				points.isEmpty() ? null : points.getLast(), null);
	}

	public List<TrackPoint> points() {
		return List.copyOf(points);
	}

	public Failure failure() {
		return failure;
	}

	public static boolean connected(TrackPoint first, RailShape firstShape, TrackPoint second, RailShape secondShape) {
		int dx = second.x() - first.x();
		int dy = second.y() - first.y();
		int dz = second.z() - first.z();
		if (Math.abs(dx) + Math.abs(dz) != 1 || Math.abs(dy) > 1) return false;
		Direction direction = dx > 0 ? Direction.EAST : dx < 0 ? Direction.WEST
				: dz > 0 ? Direction.SOUTH : Direction.NORTH;
		if (!exits(firstShape).contains(direction) || !exits(secondShape).contains(direction.getOpposite())) return false;
		if (dy > 0) return risesToward(firstShape, direction);
		if (dy < 0) return risesToward(secondShape, direction.getOpposite());
		return !risesToward(firstShape, direction) && !risesToward(secondShape, direction.getOpposite());
	}

	private static EnumSet<Direction> exits(RailShape shape) {
		return switch (shape) {
			case NORTH_SOUTH, ASCENDING_NORTH, ASCENDING_SOUTH -> EnumSet.of(Direction.NORTH, Direction.SOUTH);
			case EAST_WEST, ASCENDING_EAST, ASCENDING_WEST -> EnumSet.of(Direction.EAST, Direction.WEST);
			case SOUTH_EAST -> EnumSet.of(Direction.SOUTH, Direction.EAST);
			case SOUTH_WEST -> EnumSet.of(Direction.SOUTH, Direction.WEST);
			case NORTH_WEST -> EnumSet.of(Direction.NORTH, Direction.WEST);
			case NORTH_EAST -> EnumSet.of(Direction.NORTH, Direction.EAST);
		};
	}

	private static boolean risesToward(RailShape shape, Direction direction) {
		return shape == RailShape.ASCENDING_EAST && direction == Direction.EAST
				|| shape == RailShape.ASCENDING_WEST && direction == Direction.WEST
				|| shape == RailShape.ASCENDING_NORTH && direction == Direction.NORTH
				|| shape == RailShape.ASCENDING_SOUTH && direction == Direction.SOUTH;
	}

	public enum Reason { POSITION_JUMP, RAIL_LOST, DISCONNECTED_RAILS }
	public record Failure(Reason reason, TrackPoint previous, TrackPoint current) { }
}
