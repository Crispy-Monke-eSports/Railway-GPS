package se.railwaygps.railway.model;

import se.railwaygps.model.TrackPoint;

/** Stable boundary attachment between one physical track endpoint and one control point. */
public record TrackPort(String id, String trackId, String controlPointId, TrackPoint position, Endpoint endpoint) {
	public enum Endpoint { FROM, TO }
}
