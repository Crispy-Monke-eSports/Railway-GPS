package se.railwaygps.railway.naming;

import se.railwaygps.navigation.PolylineMatcher;
import se.railwaygps.railway.model.PhysicalTrack;
import se.railwaygps.railway.model.RailwayNetwork;
import se.railwaygps.railway.model.TrackDirection;
import se.railwaygps.railway.model.TrackRange;
import se.railwaygps.railway.model.TrackTransition;
import se.railwaygps.railway.model.TraversalRef;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.stream.Collectors;

public final class TrackPathSelector {
	private static final int MAX_PROJECTIONS = 4;
	private static final int MAX_CANDIDATES = 5;
	private static final int MAX_EXPANSIONS = 20_000;

	private TrackPathSelector() {
	}

	public static Selection select(
			RailwayNetwork network, Point start, Point end, double snapDistance) {
		if (!start.dimension().equals(end.dimension())) {
			return new Selection(List.of(), "The selected points are in different dimensions");
		}
		List<Projection> starts = projections(network, start, snapDistance);
		List<Projection> ends = projections(network, end, snapDistance);
		if (starts.isEmpty() || ends.isEmpty()) {
			return new Selection(List.of(), "A selected point is not close enough to a registered track");
		}

		List<Candidate> candidates = new ArrayList<>();
		for (Projection from : starts) {
			for (Projection to : ends) {
				candidates.addAll(findCandidates(network, from, to));
			}
		}
		List<Candidate> distinct = candidates.stream()
				.sorted(Comparator.comparingDouble(Candidate::cost))
				.collect(Collectors.toMap(Candidate::signature, candidate -> candidate,
						(first, duplicate) -> first, java.util.LinkedHashMap::new))
				.values().stream().limit(MAX_CANDIDATES).toList();
		return distinct.isEmpty()
				? new Selection(List.of(), "No permitted track path connects the selected points")
				: new Selection(distinct, null);
	}

	public static List<TrackRange> includePairedTracks(RailwayNetwork network, List<TrackRange> ranges) {
		List<TrackRange> result = new ArrayList<>(ranges);
		Set<String> present = ranges.stream().map(TrackRange::trackId).collect(Collectors.toSet());
		for (TrackRange range : ranges) {
			PhysicalTrack track = network.tracks().get(range.trackId());
			if (track == null || track.pairedTrackId() == null || present.contains(track.pairedTrackId())) {
				continue;
			}
			PhysicalTrack pair = network.tracks().get(track.pairedTrackId());
			if (pair == null) {
				continue;
			}
			double lowFraction = Math.min(range.startDistance(), range.endDistance()) / track.length();
			double highFraction = Math.max(range.startDistance(), range.endDistance()) / track.length();
			boolean reversedOrientation = track.fromControlPoint().equals(pair.toControlPoint())
					&& track.toControlPoint().equals(pair.fromControlPoint());
			TrackDirection pairedDirection = reversedOrientation ? TrackDirection.FORWARD : TrackDirection.REVERSE;
			if (!pair.directions().contains(pairedDirection)) {
				continue;
			}
			double pairedStart = reversedOrientation
					? pair.length() * (1 - highFraction) : pair.length() * highFraction;
			double pairedEnd = reversedOrientation
					? pair.length() * (1 - lowFraction) : pair.length() * lowFraction;
			result.add(new TrackRange(pair.id(), pairedStart, pairedEnd, pairedDirection));
		}
		return List.copyOf(result);
	}

	private static List<Candidate> findCandidates(RailwayNetwork network, Projection start, Projection end) {
		List<Candidate> results = new ArrayList<>();
		if (start.track().id().equals(end.track().id())) {
			addDirect(start, end, TrackDirection.FORWARD, results);
			addDirect(start, end, TrackDirection.REVERSE, results);
		}

		Map<TraversalRef, List<TrackTransition>> transitions = network.transitions().values().stream()
				.filter(TrackTransition::enabled)
				.collect(Collectors.groupingBy(TrackTransition::incoming));
		PriorityQueue<State> queue = new PriorityQueue<>(Comparator.comparingDouble(State::cost));
		seed(start, TrackDirection.FORWARD, queue);
		seed(start, TrackDirection.REVERSE, queue);
		int expansions = 0;
		while (!queue.isEmpty() && results.size() < MAX_CANDIDATES && expansions++ < MAX_EXPANSIONS) {
			State state = queue.remove();
			for (TrackTransition transition : transitions.getOrDefault(state.arrival(), List.of())) {
				TraversalRef outgoing = transition.outgoing();
				if (state.visited().contains(outgoing)) {
					continue;
				}
				PhysicalTrack track = network.tracks().get(outgoing.trackId());
				if (track == null || track.status() != PhysicalTrack.Status.VERIFIED) {
					continue;
				}
				if (track.id().equals(end.track().id())) {
					TrackRange finish = finishRange(track, outgoing.direction(), end.distanceAlong());
					results.add(candidate(state, finish, end.distanceToTrack()));
				}
				TrackRange full = fullRange(track, outgoing.direction());
				List<TrackRange> path = append(state.path(), full);
				Set<TraversalRef> visited = new HashSet<>(state.visited());
				visited.add(outgoing);
				queue.add(new State(outgoing, state.cost() + track.length(), path, Set.copyOf(visited)));
			}
		}
		return results;
	}

	private static void addDirect(Projection start, Projection end, TrackDirection direction, List<Candidate> results) {
		if (!start.track().directions().contains(direction)) {
			return;
		}
		boolean ordered = direction == TrackDirection.FORWARD
				? end.distanceAlong() >= start.distanceAlong()
				: end.distanceAlong() <= start.distanceAlong();
		if (ordered) {
			TrackRange range = new TrackRange(start.track().id(), start.distanceAlong(), end.distanceAlong(), direction);
			results.add(new Candidate(List.of(range), range.length() + start.distanceToTrack() + end.distanceToTrack()));
		}
	}

	private static void seed(Projection start, TrackDirection direction, PriorityQueue<State> queue) {
		PhysicalTrack track = start.track();
		if (!track.directions().contains(direction)) {
			return;
		}
		double end = direction == TrackDirection.FORWARD ? track.length() : 0;
		TrackRange range = new TrackRange(track.id(), start.distanceAlong(), end, direction);
		TraversalRef traversal = new TraversalRef(track.id(), direction);
		queue.add(new State(traversal, range.length() + start.distanceToTrack(), List.of(range), Set.of(traversal)));
	}

	private static Candidate candidate(State state, TrackRange finish, double endSnapDistance) {
		List<TrackRange> path = append(state.path(), finish);
		return new Candidate(path, state.cost() + finish.length() + endSnapDistance);
	}

	private static TrackRange finishRange(PhysicalTrack track, TrackDirection direction, double endDistance) {
		double start = direction == TrackDirection.FORWARD ? 0 : track.length();
		return new TrackRange(track.id(), start, endDistance, direction);
	}

	private static TrackRange fullRange(PhysicalTrack track, TrackDirection direction) {
		return direction == TrackDirection.FORWARD
				? new TrackRange(track.id(), 0, track.length(), direction)
				: new TrackRange(track.id(), track.length(), 0, direction);
	}

	private static List<TrackRange> append(List<TrackRange> path, TrackRange range) {
		List<TrackRange> result = new ArrayList<>(path);
		result.add(range);
		return List.copyOf(result);
	}

	private static List<Projection> projections(RailwayNetwork network, Point point, double snapDistance) {
		return network.tracks().values().stream()
				.filter(track -> track.status() == PhysicalTrack.Status.VERIFIED)
				.filter(track -> point.dimension().equals(track.dimension()))
				.map(track -> project(track, point))
				.filter(projection -> projection != null && projection.distanceToTrack() <= snapDistance)
				.sorted(Comparator.comparingDouble(Projection::distanceToTrack))
				.limit(MAX_PROJECTIONS)
				.toList();
	}

	private static Projection project(PhysicalTrack track, Point point) {
		PolylineMatcher.Match match = PolylineMatcher.nearest(
				track.points(), point.x(), point.y(), point.z()).orElse(null);
		if (match == null) {
			return null;
		}
		double scale = match.trackLength() == 0 ? 0 : track.length() / match.trackLength();
		return new Projection(track, match.distanceAlong() * scale, match.distanceToTrack());
	}

	public record Point(String dimension, double x, double y, double z) {
	}

	public record Selection(List<Candidate> candidates, String error) {
		public Selection {
			candidates = List.copyOf(candidates);
		}

		public boolean successful() {
			return !candidates.isEmpty();
		}
	}

	public record Candidate(List<TrackRange> ranges, double cost) {
		public Candidate {
			ranges = List.copyOf(ranges);
		}

		private String signature() {
			return ranges.stream().map(range -> range.trackId() + ":" + range.direction())
					.collect(Collectors.joining(">"));
		}
	}

	private record Projection(PhysicalTrack track, double distanceAlong, double distanceToTrack) {
	}

	private record State(TraversalRef arrival, double cost, List<TrackRange> path, Set<TraversalRef> visited) {
	}
}
