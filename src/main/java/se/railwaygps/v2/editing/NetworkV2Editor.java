package se.railwaygps.v2.editing;

import se.railwaygps.model.NetworkValidationException;
import se.railwaygps.model.NetworkValidator;
import se.railwaygps.model.TrackGeometry;
import se.railwaygps.model.TrackPoint;
import se.railwaygps.v2.model.ControlPoint;
import se.railwaygps.v2.model.NetworkV2Validator;
import se.railwaygps.v2.model.PhysicalTrack;
import se.railwaygps.v2.model.RailwayNetworkV2;
import se.railwaygps.v2.model.TrackDirection;
import se.railwaygps.v2.model.TrackSection;
import se.railwaygps.v2.model.TrackTransition;
import se.railwaygps.v2.model.TraversalRef;
import se.railwaygps.v2.model.Zone;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Immutable editing operations for the clean-break v2 network. */
public final class NetworkV2Editor {
	private NetworkV2Editor() {
	}

	public static RailwayNetworkV2 addControlPoint(
			RailwayNetworkV2 network, String id, String name, ControlPoint.Kind kind, Zone zone)
			throws NetworkValidationException {
		requireNewId(network.controlPoints(), id, "control point");
		if (name == null || name.isBlank()) {
			throw new IllegalArgumentException("Control point name is required");
		}
		Map<String, ControlPoint> points = new LinkedHashMap<>(network.controlPoints());
		points.put(id, new ControlPoint(id, name.trim(), kind, zone, Map.of()));
		return validated(copy(network, points, network.sections(), network.tracks(), network.transitions()));
	}

	public static RailwayNetworkV2 addRecordedTrack(
			RailwayNetworkV2 network, String trackId, String fromId, String toId, String dimension,
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

	public static RailwayNetworkV2 pairTracks(RailwayNetworkV2 network, String firstId, String secondId)
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
		Map<String, PhysicalTrack> tracks = new LinkedHashMap<>(network.tracks());
		tracks.put(firstId, withPair(first, secondId));
		tracks.put(secondId, withPair(second, firstId));
		return validated(copy(network, network.controlPoints(), network.sections(), tracks, network.transitions()));
	}

	public static RailwayNetworkV2 addTransition(
			RailwayNetworkV2 network, String id, String controlPointId, TraversalRef incoming,
			TraversalRef outgoing, String instruction) throws NetworkValidationException {
		requireNewId(network.transitions(), id, "transition");
		requireControlPoint(network, controlPointId);
		Map<String, TrackTransition> transitions = new LinkedHashMap<>(network.transitions());
		transitions.put(id, new TrackTransition(id, controlPointId, incoming, outgoing, true,
				instruction == null || instruction.isBlank() ? null : instruction.trim()));
		return validated(copy(network, network.controlPoints(), network.sections(), network.tracks(), transitions));
	}

	private static RailwayNetworkV2 copy(RailwayNetworkV2 network, Map<String, ControlPoint> controlPoints,
			Map<String, TrackSection> sections, Map<String, PhysicalTrack> tracks,
			Map<String, TrackTransition> transitions) {
		return new RailwayNetworkV2(network.revision() + 1, controlPoints, sections, tracks, transitions, network.labels());
	}

	private static RailwayNetworkV2 validated(RailwayNetworkV2 network) throws NetworkValidationException {
		NetworkV2Validator.validate(network);
		return network;
	}

	private static void requireNewId(Map<String, ?> objects, String id, String kind) {
		if (!NetworkValidator.isValidId(id)) {
			throw new IllegalArgumentException("Invalid " + kind + " id. Use lowercase letters, numbers, '_' or '-'.");
		}
		if (objects.containsKey(id)) {
			throw new IllegalArgumentException("A " + kind + " with id '" + id + "' already exists");
		}
	}

	private static ControlPoint requireControlPoint(RailwayNetworkV2 network, String id) {
		ControlPoint result = network.controlPoints().get(id);
		if (result == null) {
			throw new IllegalArgumentException("Unknown control point '" + id + "'");
		}
		return result;
	}

	private static PhysicalTrack requireTrack(RailwayNetworkV2 network, String id) {
		PhysicalTrack result = network.tracks().get(id);
		if (result == null) {
			throw new IllegalArgumentException("Unknown track '" + id + "'");
		}
		return result;
	}

	private static TrackSection findSection(RailwayNetworkV2 network, String first, String second) {
		return network.sections().values().stream()
				.filter(section -> section.endpointA().equals(first) && section.endpointB().equals(second)
						|| section.endpointA().equals(second) && section.endpointB().equals(first))
				.findFirst().orElse(null);
	}

	private static String newSectionId(RailwayNetworkV2 network) {
		String id;
		do {
			id = "section-" + UUID.randomUUID().toString().replace("-", "");
		} while (network.sections().containsKey(id));
		return id;
	}

	private static PhysicalTrack withPair(PhysicalTrack track, String pairId) {
		return new PhysicalTrack(track.id(), track.sectionId(), track.fromControlPoint(), track.toControlPoint(),
				track.dimension(), track.points(), track.length(), track.directions(), pairId, track.status());
	}
}
