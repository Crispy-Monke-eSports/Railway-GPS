package se.railwaygps.railway.model;

import se.railwaygps.model.NetworkValidationException;
import se.railwaygps.model.IdValidator;
import se.railwaygps.model.TrackGeometry;
import se.railwaygps.railway.editing.ZoneOverlapAnalyzer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class RailwayNetworkValidator {
	private RailwayNetworkValidator() {
	}

	public static void validate(RailwayNetwork network) throws NetworkValidationException {
		NetworkValidationReport report = inspect(network);
		if (!report.valid()) throw new NetworkValidationException(report.errors());
	}

	public static NetworkValidationReport inspect(RailwayNetwork network) {
		List<String> errors = new ArrayList<>();
		List<String> warnings = new ArrayList<>();
		Map<String, String> stationNames = new HashMap<>();
		Map<String, String> zoneIds = new HashMap<>();
		network.controlPoints().forEach((id, point) -> {
			validateIdentity("Control point", id, point == null ? null : point.id(), errors);
			if (point != null && point.zone() == null) {
				errors.add("Control point '" + id + "' has no zone");
			} else if (point != null) {
				if (point.zone().id() == null || !point.zone().id().matches("[a-z0-9_-]+")) {
					errors.add("Control point '" + id + "' has an invalid zone id");
				} else {
					String previous = zoneIds.putIfAbsent(point.zone().id(), id);
					if (previous != null) errors.add("Control points '" + previous + "' and '" + id
							+ "' share zone id '" + point.zone().id() + "'");
				}
				if (point.name() == null || point.name().isBlank()) {
					errors.add("Control point '" + id + "' has no display name");
				}
				if (point.kind() == null) errors.add("Control point '" + id + "' has no kind");
				if (point.kind() == ControlPoint.Kind.STATION && point.name() != null) {
					String normalized = point.name().trim().toLowerCase(Locale.ROOT);
					String previous = stationNames.putIfAbsent(normalized, id);
					if (previous != null) errors.add("Stations '" + previous + "' and '" + id
							+ "' use the same display name (case-insensitive): '" + point.name().trim() + "'");
				}
				if (point.kind() == ControlPoint.Kind.STATION && point.zone().boxes().size() != 1) {
					errors.add("Station '" + id + "' must use exactly one two-corner zone box");
				}
			}
		});
		List<ZoneOverlapAnalyzer.Overlap> overlaps = ZoneOverlapAnalyzer.find(network);
		for (ZoneOverlapAnalyzer.Overlap overlap : overlaps) {
			String message = "Control-point zones '" + overlap.firstControlPointId() + "' and '"
					+ overlap.secondControlPointId() + "' overlap";
			if (network.acknowledgedZoneOverlaps().contains(overlap.key())) warnings.add(message + " (acknowledged)");
			else errors.add(message + "; explicit administrator acknowledgement is required");
		}
		for (String acknowledged : network.acknowledgedZoneOverlaps()) {
			if (overlaps.stream().noneMatch(overlap -> overlap.key().equals(acknowledged))) {
				errors.add("Zone-overlap acknowledgement '" + acknowledged + "' no longer refers to overlapping zones");
			}
		}
		network.sections().forEach((id, section) -> {
			validateIdentity("Section", id, section == null ? null : section.id(), errors);
			if (section == null) {
				return;
			}
			if (!network.controlPoints().containsKey(section.endpointA())
					|| !network.controlPoints().containsKey(section.endpointB())) {
				errors.add("Section '" + id + "' refers to a missing endpoint");
			}
			for (String trackId : section.trackIds()) {
				if (!network.tracks().containsKey(trackId)) {
					errors.add("Section '" + id + "' refers to missing track '" + trackId + "'");
				}
			}
		});
		network.tracks().forEach((id, track) -> validateTrack(network, id, track, errors));
		network.transitions().forEach((id, transition) -> validateTransition(network, id, transition, errors));
		network.labels().forEach((id, label) -> validateLabel(network, id, label, errors));
		for (String rejection : network.rejectedTrackPairings()) {
			String[] ids = rejection.split("\\|", -1);
			if (ids.length != 2 || !network.tracks().containsKey(ids[0]) || !network.tracks().containsKey(ids[1])) {
				errors.add("Rejected track pairing '" + rejection + "' refers to missing tracks");
			}
		}
		warnDuplicateGeometry(network, warnings);
		return new NetworkValidationReport(errors, warnings);
	}

	private static void validateTrack(RailwayNetwork network, String id, PhysicalTrack track, List<String> errors) {
		validateIdentity("Track", id, track == null ? null : track.id(), errors);
		if (track == null) {
			return;
		}
		TrackSection section = network.sections().get(track.sectionId());
		if (section == null || !section.trackIds().contains(id)) {
			errors.add("Track '" + id + "' is not owned by its section '" + track.sectionId() + "'");
		} else if (!(sameEndpoints(section.endpointA(), section.endpointB(),
				track.fromControlPoint(), track.toControlPoint()))) {
			errors.add("Track '" + id + "' endpoints do not match section '" + section.id() + "'");
		}
		if (!network.controlPoints().containsKey(track.fromControlPoint())
				|| !network.controlPoints().containsKey(track.toControlPoint())) {
			errors.add("Track '" + id + "' refers to a missing control point");
		}
		if (track.fromControlPoint() != null && track.fromControlPoint().equals(track.toControlPoint())) {
			errors.add("Track '" + id + "' must connect different control points");
		}
		if (track.points().size() < 2 || track.points().stream().anyMatch(point -> point == null)) {
			errors.add("Track '" + id + "' must contain at least two valid points");
		}
		if (!Double.isFinite(track.length()) || track.length() <= 0) {
			errors.add("Track '" + id + "' must have positive finite length");
		} else if (track.points().size() >= 2
				&& Math.abs(track.length() - TrackGeometry.length(track.points())) > 0.01) {
			errors.add("Track '" + id + "' stored length does not match its geometry");
		}
		if (track.directions().isEmpty()) {
			errors.add("Track '" + id + "' must allow at least one direction");
		}
		if (track.pairedTrackId() != null) {
			PhysicalTrack pair = network.tracks().get(track.pairedTrackId());
			if (pair == null || !id.equals(pair.pairedTrackId())) {
				errors.add("Track '" + id + "' has a missing or non-reciprocal pair");
			}
		}
		ControlPoint from = network.controlPoints().get(track.fromControlPoint());
		ControlPoint to = network.controlPoints().get(track.toControlPoint());
		if (track.dimension() == null || track.dimension().isBlank()) {
			errors.add("Track '" + id + "' has no dimension");
		} else if (from != null && !track.dimension().equals(from.zone().dimension())
				|| to != null && !track.dimension().equals(to.zone().dimension())) {
			errors.add("Track '" + id + "' dimension does not match both endpoint zones");
		}
	}

	private static void validateTransition(
			RailwayNetwork network, String id, TrackTransition transition, List<String> errors) {
		validateIdentity("Transition", id, transition == null ? null : transition.id(), errors);
		if (transition == null) {
			return;
		}
		if (!network.controlPoints().containsKey(transition.controlPointId())) {
			errors.add("Transition '" + id + "' refers to a missing control point");
		}
		if (transition.incoming() == null || transition.outgoing() == null) {
			errors.add("Transition '" + id + "' has a missing traversal reference");
			return;
		}
		PhysicalTrack incoming = network.tracks().get(transition.incoming().trackId());
		PhysicalTrack outgoing = network.tracks().get(transition.outgoing().trackId());
		if (incoming == null || outgoing == null) {
			errors.add("Transition '" + id + "' refers to a missing track");
			return;
		}
		if (transition.incoming().direction() == null || transition.outgoing().direction() == null) {
			errors.add("Transition '" + id + "' has a missing traversal direction");
			return;
		}
		if (transition.enabled() && (incoming.status() != PhysicalTrack.Status.VERIFIED
				|| outgoing.status() != PhysicalTrack.Status.VERIFIED)) {
			errors.add("Enabled transition '" + id + "' uses a non-verified track");
		}
		if (!incoming.directions().contains(transition.incoming().direction())
				|| !outgoing.directions().contains(transition.outgoing().direction())) {
			errors.add("Transition '" + id + "' uses a forbidden track direction");
		}
		if (!endPoint(incoming, transition.incoming().direction()).equals(transition.controlPointId())
				|| !startPoint(outgoing, transition.outgoing().direction()).equals(transition.controlPointId())) {
			errors.add("Transition '" + id + "' does not meet at its control point");
		}
	}

	private static void validateLabel(RailwayNetwork network, String id, TrackLabel label, List<String> errors) {
		validateIdentity("Label", id, label == null ? null : label.id(), errors);
		if (label == null) {
			return;
		}
		if (label.name() == null || label.name().isBlank()) {
			errors.add("Label '" + id + "' has no display name");
		}
		if (label.type() == null) errors.add("Label '" + id + "' has no type");
		if (label.color() != null && (label.color() < 0 || label.color() > 0xFFFFFF)) {
			errors.add("Label '" + id + "' color must be a 24-bit RGB value");
		}
		if (label.ranges().isEmpty()) {
			errors.add("Label '" + id + "' has no track ranges");
		}
		for (TrackRange range : label.ranges()) {
			if (range == null) {
				errors.add("Label '" + id + "' contains a null interval");
				continue;
			}
			PhysicalTrack track = network.tracks().get(range.trackId());
			if (track == null) {
				errors.add("Label '" + id + "' refers to missing track '" + range.trackId() + "'");
			} else if (range.startDistance() > track.length() || range.endDistance() > track.length()) {
				errors.add("Label '" + id + "' exceeds track '" + range.trackId() + "'");
			} else if (range.length() <= 0) {
				errors.add("Label '" + id + "' has an empty interval on track '" + range.trackId() + "'");
			} else if (range.direction() == null || !track.directions().contains(range.direction())) {
				errors.add("Label '" + id + "' uses an unverified direction on track '" + range.trackId() + "'");
			} else if (track.status() != PhysicalTrack.Status.VERIFIED) {
				errors.add("Label '" + id + "' uses non-verified track '" + range.trackId() + "'");
			}
		}
	}

	private static void warnDuplicateGeometry(RailwayNetwork network, List<String> warnings) {
		List<PhysicalTrack> tracks = new ArrayList<>(network.tracks().values());
		for (int first = 0; first < tracks.size(); first++) {
			for (int second = first + 1; second < tracks.size(); second++) {
				PhysicalTrack a = tracks.get(first);
				PhysicalTrack b = tracks.get(second);
				if (Objects.equals(a.dimension(), b.dimension()) && (a.points().equals(b.points())
						|| a.points().equals(b.points().reversed()))) {
					warnings.add("Tracks '" + a.id() + "' and '" + b.id() + "' contain duplicate physical geometry");
				}
			}
		}
	}

	private static boolean sameEndpoints(String firstA, String firstB, String secondA, String secondB) {
		return Objects.equals(firstA, secondA) && Objects.equals(firstB, secondB)
				|| Objects.equals(firstA, secondB) && Objects.equals(firstB, secondA);
	}

	private static void validateIdentity(String kind, String key, String id, List<String> errors) {
		if (id == null) {
			errors.add(kind + " '" + key + "' is null");
		} else if (!IdValidator.isValid(id) || !key.equals(id)) {
			errors.add(kind + " key '" + key + "' does not match a valid id");
		}
	}

	public static String startPoint(PhysicalTrack track, TrackDirection direction) {
		return direction == TrackDirection.FORWARD ? track.fromControlPoint() : track.toControlPoint();
	}

	public static String endPoint(PhysicalTrack track, TrackDirection direction) {
		return direction == TrackDirection.FORWARD ? track.toControlPoint() : track.fromControlPoint();
	}
}
