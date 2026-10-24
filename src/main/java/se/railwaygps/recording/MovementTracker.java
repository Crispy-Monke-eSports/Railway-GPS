package se.railwaygps.recording;

/**
 * Tracks consecutive minecart positions, the travelled distance and unexpected position jumps.
 * A jump is a movement between two consecutive samples that exceeds the believable per-tick
 * movement, which cannot come from ordinary rail driving and would silently corrupt the
 * recorded geometry if it were accepted.
 */
final class MovementTracker {
	private final double maxMovementPerTick;
	private double previousX;
	private double previousY;
	private double previousZ;
	private double distance;
	private boolean jumped;

	MovementTracker(double startX, double startY, double startZ, double maxMovementPerTick) {
		this.maxMovementPerTick = maxMovementPerTick;
		this.previousX = startX;
		this.previousY = startY;
		this.previousZ = startZ;
	}

	/** Records one sampled position; believable movement accumulates as travelled distance. */
	void update(double x, double y, double z) {
		double dx = x - previousX;
		double dy = y - previousY;
		double dz = z - previousZ;
		double movement = Math.sqrt(dx * dx + dy * dy + dz * dz);
		if (movement <= maxMovementPerTick) {
			distance += movement;
		} else {
			jumped = true;
		}
		previousX = x;
		previousY = y;
		previousZ = z;
	}

	double distance() {
		return distance;
	}

	/** True when at least one update exceeded the believable per-tick movement. */
	boolean hasJumped() {
		return jumped;
	}
}
