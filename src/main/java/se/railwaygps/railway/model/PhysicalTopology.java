package se.railwaygps.railway.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Deterministic port and directed-passage projection of the authoritative physical tracks. */
public record PhysicalTopology(List<TrackPort> ports, List<DirectedPassage> passages) {
	public PhysicalTopology {
		ports = List.copyOf(ports);
		passages = List.copyOf(passages);
	}

	public static PhysicalTopology from(RailwayNetwork network) {
		List<TrackPort> ports = new ArrayList<>();
		List<DirectedPassage> passages = new ArrayList<>();
		network.tracks().values().stream().sorted(Comparator.comparing(PhysicalTrack::id)).forEach(track -> {
			if (track.points().isEmpty()) return;
			TrackPort from = new TrackPort(portId(track.id(), TrackPort.Endpoint.FROM), track.id(),
					track.fromControlPoint(), track.points().getFirst(), TrackPort.Endpoint.FROM);
			TrackPort to = new TrackPort(portId(track.id(), TrackPort.Endpoint.TO), track.id(),
					track.toControlPoint(), track.points().getLast(), TrackPort.Endpoint.TO);
			ports.add(from);
			ports.add(to);
			if (track.status() != PhysicalTrack.Status.VERIFIED) return;
			track.directions().stream().sorted().forEach(direction -> passages.add(new DirectedPassage(
					new TraversalRef(track.id(), direction),
					direction == TrackDirection.FORWARD ? from.id() : to.id(),
					direction == TrackDirection.FORWARD ? to.id() : from.id(), track.length())));
		});
		return new PhysicalTopology(ports, passages);
	}

	public static String portId(String trackId, TrackPort.Endpoint endpoint) {
		return "port-" + trackId + "-" + endpoint.name().toLowerCase(java.util.Locale.ROOT);
	}
}
