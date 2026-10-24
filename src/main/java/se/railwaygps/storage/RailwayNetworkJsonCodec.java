package se.railwaygps.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import se.railwaygps.model.NetworkValidationException;
import se.railwaygps.model.TrackPoint;
import se.railwaygps.railway.model.RailwayNetworkValidator;
import se.railwaygps.railway.model.RailwayNetwork;

import java.util.List;

public final class RailwayNetworkJsonCodec {
	public static final int SCHEMA_VERSION = 1;
	private static final Gson GSON = new GsonBuilder()
			.registerTypeAdapter(TrackPoint.class, new TrackPointAdapter())
			.setPrettyPrinting()
			.create();

	private RailwayNetworkJsonCodec() {
	}

	public static String encode(RailwayNetwork network) throws NetworkValidationException {
		RailwayNetworkValidator.validate(network);
		return GSON.toJson(new Document(SCHEMA_VERSION, network));
	}

	public static RailwayNetwork decode(String json) throws NetworkValidationException {
		try {
			Document document = GSON.fromJson(json, Document.class);
			if (document == null || document.network() == null) {
				throw new NetworkValidationException(List.of("The network document is incomplete"));
			}
			if (document.schemaVersion() != SCHEMA_VERSION) {
				throw new NetworkValidationException(List.of(
						"Unsupported Railway GPS schema " + document.schemaVersion()
								+ "; expected " + SCHEMA_VERSION + ". Automatic migration is disabled."));
			}
			RailwayNetworkValidator.validate(document.network());
			return document.network();
		} catch (JsonParseException | NullPointerException exception) {
			throw new NetworkValidationException(List.of("Invalid network document: " + exception.getMessage()));
		}
	}

	private record Document(int schemaVersion, RailwayNetwork network) {
	}
}
