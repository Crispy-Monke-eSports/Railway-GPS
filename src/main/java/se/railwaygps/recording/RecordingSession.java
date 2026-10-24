package se.railwaygps.recording;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.phys.Vec3;
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
	private Vec3 previousPosition;
	private double distance;

	RecordingSession(UUID playerId, AbstractMinecart minecart, String dimension, String from, String to, String routeId, long startedAtTick) {
		this.playerId = playerId;
		this.minecartId = minecart.getUUID();
		this.dimension = dimension;
		this.from = from;
		this.to = to;
		this.routeId = routeId;
		this.startedAtTick = startedAtTick;
		this.previousPosition = minecart.position();
		sample(minecart);
	}

	void sample(AbstractMinecart minecart) {
		Vec3 position = minecart.position();
		double movement = position.distanceTo(previousPosition);
		if (movement <= MAX_MOVEMENT_PER_TICK) {
			distance += movement;
		}
		previousPosition = position;

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
		return new RecordingStatus(routeId, from, to, points.size(), distance, currentTick - startedAtTick);
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
		return distance;
	}

	List<TrackPoint> points() {
		return List.copyOf(points);
	}
}
