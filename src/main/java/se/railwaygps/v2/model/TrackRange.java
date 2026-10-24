package se.railwaygps.v2.model;

public record TrackRange(
		String trackId,
		double startDistance,
		double endDistance,
		TrackDirection direction
) {
	public TrackRange {
		if (!Double.isFinite(startDistance) || !Double.isFinite(endDistance) || startDistance < 0 || endDistance < 0) {
			throw new IllegalArgumentException("Track range distances must be finite and non-negative");
		}
	}

	public double length() {
		return Math.abs(endDistance - startDistance);
	}
}
