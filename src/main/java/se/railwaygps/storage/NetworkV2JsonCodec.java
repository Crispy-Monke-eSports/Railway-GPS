package se.railwaygps.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import se.railwaygps.model.NetworkValidationException;
import se.railwaygps.model.TrackPoint;
import se.railwaygps.v2.model.NetworkV2Validator;
import se.railwaygps.v2.model.RailwayNetworkV2;

import java.util.List;

public final class NetworkV2JsonCodec {
	public static final int SCHEMA_VERSION = 2;
	private static final Gson GSON = new GsonBuilder()
			.registerTypeAdapter(TrackPoint.class, new TrackPointAdapter())
			.setPrettyPrinting()
			.create();

	private NetworkV2JsonCodec() {
	}

	public static String encode(RailwayNetworkV2 network) throws NetworkValidationException {
		NetworkV2Validator.validate(network);
		return GSON.toJson(new Document(SCHEMA_VERSION, network));
	}

	public static RailwayNetworkV2 decode(String json) throws NetworkValidationException {
		try {
			Document document = GSON.fromJson(json, Document.class);
			if (document == null || document.network() == null) {
				throw new NetworkValidationException(List.of("The v2 network document is incomplete"));
			}
			if (document.schemaVersion() != SCHEMA_VERSION) {
				throw new NetworkValidationException(List.of(
						"Unsupported Railway GPS schema " + document.schemaVersion()
								+ "; expected " + SCHEMA_VERSION + ". Automatic migration is disabled."));
			}
			NetworkV2Validator.validate(document.network());
			return document.network();
		} catch (JsonParseException | NullPointerException exception) {
			throw new NetworkValidationException(List.of("Invalid v2 network document: " + exception.getMessage()));
		}
	}

	private record Document(int schemaVersion, RailwayNetworkV2 network) {
	}
}
