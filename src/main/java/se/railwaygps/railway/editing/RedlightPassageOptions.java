package se.railwaygps.railway.editing;

import se.railwaygps.railway.model.PhysicalTrack;
import se.railwaygps.railway.model.RailwayNetwork;
import se.railwaygps.railway.model.RailwayNetworkValidator;
import se.railwaygps.railway.model.TrackDirection;
import se.railwaygps.railway.model.TraversalRef;

import java.util.Comparator;
import java.util.List;

/** Verified directed tracks meeting one redlight, shared by the GUI and the server. */
public final class RedlightPassageOptions {
	private RedlightPassageOptions() { }

	public static List<TraversalRef> incoming(RailwayNetwork network, String controlPointId) {
		return matching(network, controlPointId, true);
	}

	public static List<TraversalRef> outgoing(RailwayNetwork network, String controlPointId) {
		return matching(network, controlPointId, false);
	}

	private static List<TraversalRef> matching(RailwayNetwork network, String controlPointId, boolean incoming) {
		return network.tracks().values().stream()
				.filter(track -> track.status() == PhysicalTrack.Status.VERIFIED)
				.flatMap(track -> track.directions().stream().map(direction -> new TraversalRef(track.id(), direction)))
				.filter(traversal -> {
					PhysicalTrack track = network.tracks().get(traversal.trackId());
					return controlPointId.equals(incoming
							? RailwayNetworkValidator.endPoint(track, traversal.direction())
							: RailwayNetworkValidator.startPoint(track, traversal.direction()));
				})
				.sorted(Comparator.comparing(TraversalRef::trackId)
						.thenComparing(TraversalRef::direction))
				.toList();
	}
}
