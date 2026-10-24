package se.railwaygps.recording;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.level.block.BaseRailBlock;
import se.railwaygps.model.TrackPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

final class RecordingSession {
	private static final double MAX_MOVEMENT_PER_TICK = 8.0;

	private final UUID playerId;
	private final UUID minecartId;
	private final String dimension;
	private final String from;
	private final String to;
	private final String routeId;
	private final long startedAtTick;
	private final List<TrackPoint> points = new ArrayList<>();
	private final MovementTracker movement;

	RecordingSession(UUID playerId, AbstractMinecart minecart, String dimension, String from, String to, String routeId, long startedAtTick) {
		this.playerId = playerId;
		this.minecartId = minecart.getUUID();
		this.dimension = dimension;
		this.from = from;
		this.to = to;
		this.routeId = routeId;
		this.startedAtTick = startedAtTick;
		this.movement = new MovementTracker(minecart.getX(), minecart.getY(), minecart.getZ(), MAX_MOVEMENT_PER_TICK);
		sample(minecart);
	}

	void sample(AbstractMinecart minecart) {
		movement.update(minecart.getX(), minecart.getY(), minecart.getZ());

		BlockPos railPosition = minecart.getCurrentBlockPosOrRailBelow();
		if (!BaseRailBlock.isRail(minecart.level(), railPosition)) {
			return;
		}
		TrackPoint point = new TrackPoint(railPosition.getX(), railPosition.getY(), railPosition.getZ());
		if (points.isEmpty() || !points.getLast().equals(point)) {
			points.add(point);
		}
	}

	RecordingStatus status(long currentTick) {
		return new RecordingStatus(routeId, from, to, points.size(), distance(), currentTick - startedAtTick);
	}

	UUID playerId() {
		return playerId;
	}

	UUID minecartId() {
		return minecartId;
	}

	String dimension() {
		return dimension;
	}

	String from() {
		return from;
	}

	String to() {
		return to;
	}

	String routeId() {
		return routeId;
	}

	double distance() {
		return movement.distance();
	}

	/** True when the minecart position jumped between two samples, which makes the recording unusable. */
	boolean hasPositionJump() {
		return movement.hasJumped();
	}

	List<TrackPoint> points() {
		return List.copyOf(points);
	}
}
