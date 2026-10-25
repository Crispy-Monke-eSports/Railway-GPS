package se.railwaygps.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import se.railwaygps.model.NetworkValidationException;
import se.railwaygps.model.TrackPoint;
import se.railwaygps.railway.model.RailwayNetwork;
import se.railwaygps.railway.model.RailwayNetworkValidator;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Validates the envelope before asking Gson to construct strict railway records. */
public final class RailwayNetworkJsonCodec {
	public static final int SCHEMA_VERSION = 2;
	private static final int LEGACY_SCHEMA_VERSION = 1;
	private static final int LEGACY_CONFIRMATION_TICKS = 3;
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
			JsonElement parsed = JsonParser.parseString(json);
			if (parsed == null || !parsed.isJsonObject()) {
				throw new NetworkValidationException(List.of("The railway network document must be a JSON object"));
			}
			JsonObject documentJson = parsed.getAsJsonObject();
			JsonElement versionValue = documentJson.get("schemaVersion");
			if (versionValue == null || !versionValue.isJsonPrimitive()
					|| !versionValue.getAsJsonPrimitive().isNumber()) {
				throw new NetworkValidationException(List.of("Missing or invalid railway network schemaVersion"));
			}
			String versionText = versionValue.getAsString();
			if (!versionText.equals("1") && !versionText.equals("2")) {
				throw new NetworkValidationException(List.of("Unsupported Railway GPS schema "
						+ versionText + "; supported versions are 1 and " + SCHEMA_VERSION));
			}

			JsonElement networkJson = documentJson.get("network");
			if (networkJson == null || !networkJson.isJsonObject()) {
				throw new NetworkValidationException(List.of("The railway network document is incomplete"));
			}

			// Schema 1 zones have no id/confirmationTicks. Only supply defaults for
			// genuinely absent legacy fields; explicit null/invalid values stay invalid.
			if (versionText.equals("1")) {
				migrateLegacyZones(networkJson.getAsJsonObject());
			}
			Document document = GSON.fromJson(documentJson, Document.class);
			if (document == null || document.network() == null) {
				throw new NetworkValidationException(List.of("The railway network document is incomplete"));
			}
			RailwayNetworkValidator.validate(document.network());
			return document.network();
		} catch (RuntimeException exception) {
			// Gson can wrap an IllegalArgumentException thrown by a record's canonical
			// constructor in RuntimeException (not necessarily JsonParseException).
			// Never include user-controlled JSON or local paths in the error message.
			throw new NetworkValidationException(List.of(
					"Invalid railway network document (malformed zone or field data); restore a valid backup"));
		}
	}

	private static void migrateLegacyZones(JsonObject networkJson) throws NetworkValidationException {
		JsonElement controlPoints = networkJson.get("controlPoints");
		if (controlPoints == null || controlPoints.isJsonNull()) return;
		if (!controlPoints.isJsonObject()) {
			throw new NetworkValidationException(List.of("Invalid legacy controlPoints object"));
		}
		for (Map.Entry<String, JsonElement> entry : controlPoints.getAsJsonObject().entrySet()) {
			if (!entry.getValue().isJsonObject()) continue;
			JsonObject point = entry.getValue().getAsJsonObject();
			migrateZone(point.get("zone"), "control-point/" + entry.getKey());
			JsonElement platforms = point.get("platforms");
			if (platforms != null && platforms.isJsonObject()) {
				for (Map.Entry<String, JsonElement> platform : platforms.getAsJsonObject().entrySet()) {
					if (platform.getValue().isJsonObject()) {
						migrateZone(platform.getValue().getAsJsonObject().get("zone"),
								"control-point/" + entry.getKey() + "/platform/" + platform.getKey());
					}
				}
			}
		}
	}

	private static void migrateZone(JsonElement value, String sourceKey) {
		if (value == null || !value.isJsonObject()) return;
		JsonObject zone = value.getAsJsonObject();
		if (!zone.has("id")) {
			String stableId = UUID.nameUUIDFromBytes(sourceKey.getBytes(StandardCharsets.UTF_8))
					.toString().replace("-", "");
			zone.addProperty("id", "zone-" + stableId);
		}
		if (!zone.has("confirmationTicks")) {
			zone.addProperty("confirmationTicks", LEGACY_CONFIRMATION_TICKS);
		}
	}

	private record Document(int schemaVersion, RailwayNetwork network) {
	}
}
