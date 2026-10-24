package se.railwaygps.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import se.railwaygps.model.Junction;
import se.railwaygps.model.NetworkValidationException;
import se.railwaygps.model.NetworkValidator;
import se.railwaygps.model.RailwayNetwork;
import se.railwaygps.model.Route;
import se.railwaygps.model.Station;
import se.railwaygps.model.TrackPoint;

import java.util.Map;
import java.util.TreeMap;

/** Stable wire representation of a fully validated network snapshot. */
public final class NetworkJsonCodec {
	private static final Gson GSON = new GsonBuilder()
			.registerTypeAdapter(TrackPoint.class, new TrackPointAdapter())
			.create();

	private NetworkJsonCodec() {
	}

	public static String encode(RailwayNetwork network) {
		Snapshot snapshot = new Snapshot(
				new TreeMap<>(network.stations()),
				new TreeMap<>(network.junctions()),
				new TreeMap<>(network.routes()));
		return GSON.toJson(snapshot);
	}

	public static RailwayNetwork decode(String json) throws NetworkValidationException {
		try {
			Snapshot snapshot = GSON.fromJson(json, Snapshot.class);
			if (snapshot == null || snapshot.stations() == null || snapshot.junctions() == null || snapshot.routes() == null) {
				throw new NetworkValidationException(java.util.List.of("Network snapshot is incomplete"));
			}
			RailwayNetwork network = new RailwayNetwork(snapshot.stations(), snapshot.junctions(), snapshot.routes());
			NetworkValidator.validate(network);
			return network;
		} catch (JsonParseException | NullPointerException exception) {
			throw new NetworkValidationException(java.util.List.of("Invalid network snapshot: " + exception.getMessage()));
		}
	}

	private record Snapshot(
			Map<String, Station> stations,
			Map<String, Junction> junctions,
			Map<String, Route> routes
	) {
	}
}
