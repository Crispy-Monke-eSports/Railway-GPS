package se.railwaygps.v2.naming;

import se.railwaygps.model.NetworkValidationException;
import se.railwaygps.model.NetworkValidator;
import se.railwaygps.v2.model.NetworkV2Validator;
import se.railwaygps.v2.model.RailwayNetworkV2;
import se.railwaygps.v2.model.TrackLabel;
import se.railwaygps.v2.model.TrackRange;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TrackLabelEditor {
	private TrackLabelEditor() {
	}

	public static Preview preview(
			RailwayNetworkV2 network,
			TrackPathSelector.Point start,
			TrackPathSelector.Point end,
			double snapDistance) {
		TrackPathSelector.Selection selection = TrackPathSelector.select(network, start, end, snapDistance);
		return new Preview(selection.candidates(), selection.error());
	}

	public static RailwayNetworkV2 apply(
			RailwayNetworkV2 network,
			String id,
			String name,
			TrackLabel.Type type,
			int priority,
			Integer color,
			Preview preview,
			int candidateIndex,
			boolean includePairedTracks
	) throws NetworkValidationException {
		if (!NetworkValidator.isValidId(id)) {
			throw new IllegalArgumentException("Invalid label id '" + id + "'");
		}
		if (name == null || name.isBlank()) {
			throw new IllegalArgumentException("Label display name is required");
		}
		if (network.labels().containsKey(id)) {
			throw new IllegalArgumentException("Label '" + id + "' already exists");
		}
		if (candidateIndex < 0 || candidateIndex >= preview.candidates().size()) {
			throw new IllegalArgumentException("The selected path candidate does not exist");
		}

		List<TrackRange> ranges = preview.candidates().get(candidateIndex).ranges();
		if (includePairedTracks) {
			ranges = TrackPathSelector.includePairedTracks(network, ranges);
		}
		Map<String, TrackLabel> labels = new LinkedHashMap<>(network.labels());
		labels.put(id, new TrackLabel(id, name.trim(), type, priority, color, ranges));
		RailwayNetworkV2 updated = new RailwayNetworkV2(network.revision() + 1,
				network.controlPoints(), network.sections(), network.tracks(), network.transitions(), labels);
		NetworkV2Validator.validate(updated);
		return updated;
	}

	public record Preview(List<TrackPathSelector.Candidate> candidates, String error) {
		public Preview {
			candidates = List.copyOf(candidates);
		}

		public boolean successful() {
			return !candidates.isEmpty();
		}

		public boolean requiresChoice() {
			return candidates.size() > 1;
		}
	}
}
