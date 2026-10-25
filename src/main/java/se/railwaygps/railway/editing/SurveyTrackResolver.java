package se.railwaygps.railway.editing;

import se.railwaygps.model.NetworkValidationException;
import se.railwaygps.model.TrackGeometry;
import se.railwaygps.model.TrackPoint;
import se.railwaygps.navigation.PolylineMatcher;
import se.railwaygps.railway.model.PhysicalTrack;
import se.railwaygps.railway.model.RailwayNetwork;
import se.railwaygps.railway.model.TrackDirection;
import se.railwaygps.railway.model.TraversalRef;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Resolves surveys against existing physical geometry without guessing or cloning it. */
public final class SurveyTrackResolver {
	private static final double GEOMETRY_TOLERANCE = 0.35;
	private static final double LENGTH_TOLERANCE = 0.5;

	private SurveyTrackResolver() { }

	public static Anchor anchor(RailwayNetwork network, String dimension, TrackPoint point) {
		List<Anchor> matches = network.tracks().values().stream()
				.filter(track -> track.status() == PhysicalTrack.Status.VERIFIED)
				.filter(track -> dimension.equals(track.dimension()))
				.map(track -> new Anchor(track.id(), distance(track.points(), point)))
				.filter(anchor -> anchor.distance() <= GEOMETRY_TOLERANCE)
				.sorted(Comparator.comparingDouble(Anchor::distance))
				.toList();
		if (matches.isEmpty()) throw new IllegalArgumentException(
				"The starting rail is not attached to a registered physical track");
		if (matches.size() > 1 && matches.get(1).distance() <= matches.getFirst().distance() + 0.05) {
			throw new IllegalArgumentException("The starting rail matches multiple physical tracks");
		}
		return matches.getFirst();
	}

	public static TraversalRef resolveAnchorTraversal(RailwayNetwork network, Anchor anchor,
			String destinationControlPoint, List<TrackPoint> observedPrefix) {
		PhysicalTrack track = network.tracks().get(anchor.trackId());
		if (track == null) throw new IllegalArgumentException("The anchored physical track no longer exists");
		List<TrackDirection> directions = new ArrayList<>();
		if (track.toControlPoint().equals(destinationControlPoint)) directions.add(TrackDirection.FORWARD);
		if (track.fromControlPoint().equals(destinationControlPoint)) directions.add(TrackDirection.REVERSE);
		directions.removeIf(direction -> !track.directions().contains(direction)
				|| !follows(track, direction, observedPrefix));
		if (directions.size() != 1) throw new IllegalArgumentException(directions.isEmpty()
				? "The observed start cannot be safely attached to an approved track direction"
				: "The observed start direction is ambiguous");
		return new TraversalRef(track.id(), directions.getFirst());
	}

	public static ResolvedTraversal resolveOrCreate(RailwayNetwork network, String fromId, String toId,
			String dimension, List<TrackPoint> rawPoints) throws NetworkValidationException {
		List<TrackPoint> points = TrackGeometry.simplify(rawPoints);
		List<PhysicalTrack> matches = network.tracks().values().stream()
				.filter(track -> dimension.equals(track.dimension()))
				.filter(track -> sameEndpoints(track, fromId, toId))
				.filter(track -> equivalentGeometry(track.points(), points))
				.toList();
		if (matches.size() > 1) {
			throw new IllegalArgumentException("Recorded geometry ambiguously matches multiple physical tracks");
		}
		if (matches.size() == 1) {
			PhysicalTrack track = matches.getFirst();
			TrackDirection direction = track.fromControlPoint().equals(fromId)
					? TrackDirection.FORWARD : TrackDirection.REVERSE;
			RailwayNetwork updated = track.directions().contains(direction) ? network
					: RailwayNetworkEditor.allowTrackDirection(network, track.id(), direction);
			return new ResolvedTraversal(updated, new TraversalRef(track.id(), direction), false);
		}
		RailwayNetworkEditor.AddedTrack added = RailwayNetworkEditor.addRecordedTrack(
				network, fromId, toId, dimension, points);
		return new ResolvedTraversal(added.network(),
				new TraversalRef(added.trackId(), TrackDirection.FORWARD), true);
	}

	static boolean equivalentGeometry(List<TrackPoint> first, List<TrackPoint> second) {
		if (first.size() < 2 || second.size() < 2
				|| Math.abs(TrackGeometry.length(first) - TrackGeometry.length(second)) > LENGTH_TOLERANCE) return false;
		return allNear(first, second) && allNear(second, first);
	}

	private static boolean allNear(List<TrackPoint> samples, List<TrackPoint> geometry) {
		return samples.stream().allMatch(point -> distance(geometry, point) <= GEOMETRY_TOLERANCE);
	}

	private static boolean follows(PhysicalTrack track, TrackDirection direction, List<TrackPoint> observations) {
		if (observations.isEmpty()) return false;
		double previous = direction == TrackDirection.FORWARD ? -Double.MAX_VALUE : Double.MAX_VALUE;
		for (TrackPoint point : observations) {
			PolylineMatcher.Match match = PolylineMatcher.nearest(track.points(),
					point.x() + 0.5, point.y() + 0.5, point.z() + 0.5).orElse(null);
			if (match == null || match.distanceToTrack() > GEOMETRY_TOLERANCE) return false;
			if (direction == TrackDirection.FORWARD && match.distanceAlong() + 0.05 < previous) return false;
			if (direction == TrackDirection.REVERSE && match.distanceAlong() - 0.05 > previous) return false;
			previous = match.distanceAlong();
		}
		return direction == TrackDirection.FORWARD
				? previous >= TrackGeometry.length(track.points()) - 2.0
				: previous <= 2.0;
	}

	private static boolean sameEndpoints(PhysicalTrack track, String first, String second) {
		return track.fromControlPoint().equals(first) && track.toControlPoint().equals(second)
				|| track.fromControlPoint().equals(second) && track.toControlPoint().equals(first);
	}

	private static double distance(List<TrackPoint> geometry, TrackPoint point) {
		return PolylineMatcher.nearest(geometry, point.x() + 0.5, point.y() + 0.5, point.z() + 0.5)
				.map(PolylineMatcher.Match::distanceToTrack).orElse(Double.POSITIVE_INFINITY);
	}

	public record Anchor(String trackId, double distance) { }
	public record ResolvedTraversal(RailwayNetwork network, TraversalRef traversal, boolean created) { }
}
