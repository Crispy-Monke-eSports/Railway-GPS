package se.railwaygps.navigation;

import se.railwaygps.railway.model.RailwayNetwork;
import se.railwaygps.railway.model.TrackDirection;
import se.railwaygps.railway.model.TrackLabel;
import se.railwaygps.railway.model.TrackRange;

import java.util.Comparator;
import java.util.Optional;

/** Selects at most one main way and one local interval at a confirmed directed rail position. */
public final class RailLabelResolver {
	private RailLabelResolver() { }

	public static Names at(RailwayNetwork network, String trackId, TrackDirection direction, double distance) {
		Comparator<TrackLabel> priority = Comparator.comparingInt(TrackLabel::priority).reversed()
				.thenComparing(TrackLabel::name, String.CASE_INSENSITIVE_ORDER).thenComparing(TrackLabel::id);
		Optional<TrackLabel> way = network.labels().values().stream()
				.filter(label -> label.type() == TrackLabel.Type.WAY && applies(label, trackId, direction, distance))
				.sorted(priority).findFirst();
		Optional<TrackLabel> local = network.labels().values().stream()
				.filter(label -> label.type() != TrackLabel.Type.WAY && applies(label, trackId, direction, distance))
				.sorted(priority).findFirst();
		return new Names(way.map(TrackLabel::name).orElse(null), local.map(TrackLabel::name).orElse(null));
	}

	private static boolean applies(TrackLabel label, String trackId, TrackDirection direction, double distance) {
		return label.ranges().stream().anyMatch(range -> range.trackId().equals(trackId)
				&& range.direction() == direction && contains(range, distance));
	}

	private static boolean contains(TrackRange range, double distance) {
		return distance >= Math.min(range.startDistance(), range.endDistance())
				&& distance <= Math.max(range.startDistance(), range.endDistance());
	}

	public record Names(String way, String local) {
		public String display() {
			if (way == null) return local == null ? "" : local;
			return local == null ? way : way + " · " + local;
		}
	}
}
