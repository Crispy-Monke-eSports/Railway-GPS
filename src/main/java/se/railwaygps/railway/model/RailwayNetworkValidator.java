package se.railwaygps.railway.model;

import se.railwaygps.model.NetworkValidationException;
import se.railwaygps.model.IdValidator;

import java.util.ArrayList;
import java.util.List;

public final class RailwayNetworkValidator {
	private RailwayNetworkValidator() {
	}

	public static void validate(RailwayNetwork network) throws NetworkValidationException {
		List<String> errors = new ArrayList<>();
		network.controlPoints().forEach((id, point) -> {
			validateIdentity("Control point", id, point == null ? null : point.id(), errors);
			if (point != null && point.zone() == null) {
				errors.add("Control point '" + id + "' has no zone");
			}
		});
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
		if (!errors.isEmpty()) {
			throw new NetworkValidationException(errors);
		}
	}

	private static void validateTrack(RailwayNetwork network, String id, PhysicalTrack track, List<String> errors) {
		validateIdentity("Track", id, track == null ? null : track.id(), errors);
		if (track == null) {
			return;
		}
		TrackSection section = network.sections().get(track.sectionId());
		if (section == null || !section.trackIds().contains(id)) {
			errors.add("Track '" + id + "' is not owned by its section '" + track.sectionId() + "'");
		}
		if (!network.controlPoints().containsKey(track.fromControlPoint())
				|| !network.controlPoints().containsKey(track.toControlPoint())) {
			errors.add("Track '" + id + "' refers to a missing control point");
		}
		if (track.fromControlPoint().equals(track.toControlPoint())) {
			errors.add("Track '" + id + "' must connect different control points");
		}
		if (track.points().size() < 2 || track.points().stream().anyMatch(point -> point == null)) {
			errors.add("Track '" + id + "' must contain at least two valid points");
		}
		if (!Double.isFinite(track.length()) || track.length() <= 0) {
			errors.add("Track '" + id + "' must have positive finite length");
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
		PhysicalTrack incoming = network.tracks().get(transition.incoming().trackId());
		PhysicalTrack outgoing = network.tracks().get(transition.outgoing().trackId());
		if (incoming == null || outgoing == null) {
			errors.add("Transition '" + id + "' refers to a missing track");
			return;
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
		if (label.ranges().isEmpty()) {
			errors.add("Label '" + id + "' has no track ranges");
		}
		for (TrackRange range : label.ranges()) {
			PhysicalTrack track = network.tracks().get(range.trackId());
			if (track == null) {
				errors.add("Label '" + id + "' refers to missing track '" + range.trackId() + "'");
			} else if (range.startDistance() > track.length() || range.endDistance() > track.length()) {
				errors.add("Label '" + id + "' exceeds track '" + range.trackId() + "'");
			}
		}
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
