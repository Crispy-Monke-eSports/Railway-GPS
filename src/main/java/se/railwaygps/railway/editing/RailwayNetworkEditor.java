package se.railwaygps.railway.editing;

import se.railwaygps.model.NetworkValidationException;
import se.railwaygps.model.IdValidator;
import se.railwaygps.model.TrackGeometry;
import se.railwaygps.model.TrackPoint;
import se.railwaygps.railway.model.ControlPoint;
import se.railwaygps.railway.model.RailwayNetworkValidator;
import se.railwaygps.railway.model.PhysicalTrack;
import se.railwaygps.railway.model.RailwayNetwork;
import se.railwaygps.railway.model.TrackDirection;
import se.railwaygps.railway.model.TrackSection;
import se.railwaygps.railway.model.TrackTransition;
import se.railwaygps.railway.model.TraversalRef;
import se.railwaygps.railway.model.Zone;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Immutable editing operations for the physical railway network. */
public final class RailwayNetworkEditor {
	private RailwayNetworkEditor() {
	}

	public static RailwayNetwork addControlPoint(
			RailwayNetwork network, String id, String name, ControlPoint.Kind kind, Zone zone)
			throws NetworkValidationException {
		requireNewId(network.controlPoints(), id, "control point");
		if (name == null || name.isBlank()) {
			throw new IllegalArgumentException("Control point name is required");
		}
		Map<String, ControlPoint> points = new LinkedHashMap<>(network.controlPoints());
		points.put(id, new ControlPoint(id, name.trim(), kind, zone, Map.of()));
		return validated(copy(network, points, network.sections(), network.tracks(), network.transitions()));
	}

	public static RailwayNetwork addRecordedTrack(
			RailwayNetwork network, String trackId, String fromId, String toId, String dimension,
			List<TrackPoint> rawPoints) throws NetworkValidationException {
		requireNewId(network.tracks(), trackId, "track");
		ControlPoint from = requireControlPoint(network, fromId);
		ControlPoint to = requireControlPoint(network, toId);
		if (fromId.equals(toId)) {
			throw new IllegalArgumentException("Track endpoints must be different control points");
		}
		if (!dimension.equals(from.zone().dimension()) || !dimension.equals(to.zone().dimension())) {
			throw new IllegalArgumentException("Track and both control points must be in the same dimension");
		}
		List<TrackPoint> points = TrackGeometry.simplify(rawPoints);
		double length = TrackGeometry.length(points);
		if (points.size() < 2 || length <= 0) {
			throw new IllegalArgumentException("At least two distinct rail points are required");
		}

		Map<String, TrackSection> sections = new LinkedHashMap<>(network.sections());
		TrackSection section = findSection(network, fromId, toId);
		if (section == null) {
			String sectionId = newSectionId(network);
			section = new TrackSection(sectionId, fromId, toId, Set.of(trackId));
		} else {
			Set<String> trackIds = new LinkedHashSet<>(section.trackIds());
			trackIds.add(trackId);
			section = new TrackSection(section.id(), section.endpointA(), section.endpointB(), trackIds);
		}
		sections.put(section.id(), section);

		Map<String, PhysicalTrack> tracks = new LinkedHashMap<>(network.tracks());
		tracks.put(trackId, new PhysicalTrack(trackId, section.id(), fromId, toId, dimension, points, length,
				Set.of(TrackDirection.FORWARD), null, PhysicalTrack.Status.VERIFIED));
		return validated(copy(network, network.controlPoints(), sections, tracks, network.transitions()));
	}

	/** Adds a recorded track with an opaque identity owned by Railway GPS. */
	public static AddedTrack addRecordedTrack(
			RailwayNetwork network, String fromId, String toId, String dimension,
			List<TrackPoint> rawPoints) throws NetworkValidationException {
		String trackId = newTrackId(network);
		RailwayNetwork updated = addRecordedTrack(network, trackId, fromId, toId, dimension, rawPoints);
		return new AddedTrack(updated, trackId);
	}

	public static RailwayNetwork pairTracks(RailwayNetwork network, String firstId, String secondId)
			throws NetworkValidationException {
		if (firstId.equals(secondId)) {
			throw new IllegalArgumentException("A track cannot be paired with itself");
		}
		PhysicalTrack first = requireTrack(network, firstId);
		PhysicalTrack second = requireTrack(network, secondId);
		if (!first.sectionId().equals(second.sectionId())) {
			throw new IllegalArgumentException("Paired tracks must belong to the same section");
		}
		if (!first.fromControlPoint().equals(second.toControlPoint())
				|| !first.toControlPoint().equals(second.fromControlPoint())) {
			throw new IllegalArgumentException("Paired directional tracks must have opposite endpoints");
		}
		if (first.pairedTrackId() != null || second.pairedTrackId() != null) {
			throw new IllegalArgumentException("One of the tracks is already paired");
		}
		if (TrackPairingAnalyzer.analyze(first, second).isEmpty()) {
			throw new IllegalArgumentException("Tracks are not a compatible geometric parallel pair");
		}
		Map<String, PhysicalTrack> tracks = new LinkedHashMap<>(network.tracks());
		tracks.put(firstId, withPair(first, secondId));
		tracks.put(secondId, withPair(second, firstId));
		Set<String> rejections = new LinkedHashSet<>(network.rejectedTrackPairings());
		rejections.remove(TrackPairingAnalyzer.pairKey(firstId, secondId));
		return validated(copy(network, network.controlPoints(), network.sections(), tracks, network.transitions(), rejections));
	}

	public static RailwayNetwork rejectTrackPairing(RailwayNetwork network, String firstId, String secondId)
			throws NetworkValidationException {
		PhysicalTrack first = requireTrack(network, firstId);
		PhysicalTrack second = requireTrack(network, secondId);
		if (TrackPairingAnalyzer.analyze(first, second).isEmpty()) {
			throw new IllegalArgumentException("Tracks are not a current geometric pairing proposal");
		}
		Set<String> rejections = new LinkedHashSet<>(network.rejectedTrackPairings());
		rejections.add(TrackPairingAnalyzer.pairKey(firstId, secondId));
		return validated(copy(network, network.controlPoints(), network.sections(), network.tracks(),
				network.transitions(), rejections));
	}

	public static RailwayNetwork allowTrackDirection(
			RailwayNetwork network, String trackId, TrackDirection direction) throws NetworkValidationException {
		PhysicalTrack track = requireTrack(network, trackId);
		Set<TrackDirection> directions = new LinkedHashSet<>(track.directions());
		if (!directions.add(direction)) throw new IllegalArgumentException("Track direction is already verified");
		Map<String, PhysicalTrack> tracks = new LinkedHashMap<>(network.tracks());
		tracks.put(trackId, new PhysicalTrack(track.id(), track.sectionId(), track.fromControlPoint(),
				track.toControlPoint(), track.dimension(), track.points(), track.length(), directions,
				track.pairedTrackId(), track.status()));
		return validated(copy(network, network.controlPoints(), network.sections(), tracks, network.transitions()));
	}

	public static RailwayNetwork addTransition(
			RailwayNetwork network, String id, String controlPointId, TraversalRef incoming,
			TraversalRef outgoing, String instruction) throws NetworkValidationException {
		requireNewId(network.transitions(), id, "transition");
		requireControlPoint(network, controlPointId);
		Map<String, TrackTransition> transitions = new LinkedHashMap<>(network.transitions());
		transitions.put(id, new TrackTransition(id, controlPointId, incoming, outgoing, true,
				instruction == null || instruction.isBlank() ? null : instruction.trim()));
		return validated(copy(network, network.controlPoints(), network.sections(), network.tracks(), transitions));
	}

	private static RailwayNetwork copy(RailwayNetwork network, Map<String, ControlPoint> controlPoints,
			Map<String, TrackSection> sections, Map<String, PhysicalTrack> tracks,
			Map<String, TrackTransition> transitions) {
		return copy(network, controlPoints, sections, tracks, transitions, network.rejectedTrackPairings());
	}

	private static RailwayNetwork copy(RailwayNetwork network, Map<String, ControlPoint> controlPoints,
			Map<String, TrackSection> sections, Map<String, PhysicalTrack> tracks,
			Map<String, TrackTransition> transitions, Set<String> rejectedTrackPairings) {
		return new RailwayNetwork(network.revision() + 1, controlPoints, sections, tracks, transitions,
				network.labels(), rejectedTrackPairings);
	}

	private static RailwayNetwork validated(RailwayNetwork network) throws NetworkValidationException {
		RailwayNetworkValidator.validate(network);
		return network;
	}

	private static void requireNewId(Map<String, ?> objects, String id, String kind) {
		if (!IdValidator.isValid(id)) {
			throw new IllegalArgumentException("Invalid " + kind + " id. Use lowercase letters, numbers, '_' or '-'.");
		}
		if (objects.containsKey(id)) {
			throw new IllegalArgumentException("A " + kind + " with id '" + id + "' already exists");
		}
	}

	private static ControlPoint requireControlPoint(RailwayNetwork network, String id) {
		ControlPoint result = network.controlPoints().get(id);
		if (result == null) {
			throw new IllegalArgumentException("Unknown control point '" + id + "'");
		}
		return result;
	}

	private static PhysicalTrack requireTrack(RailwayNetwork network, String id) {
		PhysicalTrack result = network.tracks().get(id);
		if (result == null) {
			throw new IllegalArgumentException("Unknown track '" + id + "'");
		}
		return result;
	}

	private static TrackSection findSection(RailwayNetwork network, String first, String second) {
		return network.sections().values().stream()
				.filter(section -> section.endpointA().equals(first) && section.endpointB().equals(second)
						|| section.endpointA().equals(second) && section.endpointB().equals(first))
				.findFirst().orElse(null);
	}

	private static String newSectionId(RailwayNetwork network) {
		String id;
		do {
			id = "section-" + UUID.randomUUID().toString().replace("-", "");
		} while (network.sections().containsKey(id));
		return id;
	}

	private static String newTrackId(RailwayNetwork network) {
		String id;
		do {
			id = "track-" + UUID.randomUUID().toString().replace("-", "");
		} while (network.tracks().containsKey(id));
		return id;
	}

	private static PhysicalTrack withPair(PhysicalTrack track, String pairId) {
		return new PhysicalTrack(track.id(), track.sectionId(), track.fromControlPoint(), track.toControlPoint(),
				track.dimension(), track.points(), track.length(), track.directions(), pairId, track.status());
	}

	public record AddedTrack(RailwayNetwork network, String trackId) {
	}
}
