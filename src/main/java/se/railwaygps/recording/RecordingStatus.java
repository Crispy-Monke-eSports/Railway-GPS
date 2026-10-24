package se.railwaygps.recording;

public record RecordingStatus(
		String routeId,
		String from,
		String to,
		int pointCount,
		double distance,
		long elapsedTicks
) {
}
