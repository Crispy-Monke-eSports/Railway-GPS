package se.railwaygps.navigation;

import se.railwaygps.model.TrackPoint;
import se.railwaygps.railway.model.RailwayNetworkValidator;
import se.railwaygps.railway.model.PhysicalTrack;
import se.railwaygps.railway.model.TrackDirection;
import se.railwaygps.railway.model.TraversalRef;

import java.util.List;

public record PhysicalRouteLeg(PhysicalTrack track, TrackDirection direction, String from, String to) {
	public PhysicalRouteLeg(PhysicalTrack track, TrackDirection direction) {
		this(track, direction, RailwayNetworkValidator.startPoint(track, direction),
				RailwayNetworkValidator.endPoint(track, direction));
	}

	public TraversalRef traversal() {
		return new TraversalRef(track.id(), direction);
	}

	public List<TrackPoint> points() {
		return direction == TrackDirection.FORWARD ? track.points() : track.points().reversed();
	}
}
