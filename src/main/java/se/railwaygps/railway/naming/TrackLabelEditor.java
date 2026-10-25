package se.railwaygps.railway.naming;

import se.railwaygps.model.NetworkValidationException;
import se.railwaygps.model.IdValidator;
import se.railwaygps.railway.model.RailwayNetworkValidator;
import se.railwaygps.railway.model.RailwayNetwork;
import se.railwaygps.railway.model.TrackLabel;
import se.railwaygps.railway.model.TrackRange;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TrackLabelEditor {
	private TrackLabelEditor() {
	}

	public static Preview preview(
			RailwayNetwork network,
			TrackPathSelector.Point start,
			TrackPathSelector.Point end,
			double snapDistance) {
		TrackPathSelector.Selection selection = TrackPathSelector.select(network, start, end, snapDistance);
		return new Preview(selection.candidates(), selection.error());
	}

	public static RailwayNetwork apply(
			RailwayNetwork network,
			String id,
			String name,
			TrackLabel.Type type,
			int priority,
			Integer color,
			Preview preview,
			int candidateIndex,
			boolean includePairedTracks
	) throws NetworkValidationException {
		if (!IdValidator.isValid(id)) {
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
		RailwayNetwork updated = new RailwayNetwork(network.revision() + 1,
				network.controlPoints(), network.sections(), network.tracks(), network.transitions(), labels,
				network.rejectedTrackPairings());
		RailwayNetworkValidator.validate(updated);
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
