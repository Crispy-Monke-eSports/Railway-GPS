package se.railwaygps.storage;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import se.railwaygps.model.NetworkValidationException;
import se.railwaygps.model.TrackPoint;
import se.railwaygps.railway.model.ControlPoint;
import se.railwaygps.railway.model.RailwayNetwork;
import se.railwaygps.railway.model.Zone;
import se.railwaygps.railway.model.ZoneBox;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RailwayNetworkJsonCodecTest {
	@Test
	void migratesSchemaOneZonesWithoutLosingGeometryOrIdentity() throws Exception {
		JsonObject oldDocument = asJson(network());
		oldDocument.addProperty("schemaVersion", 1);
		for (String id : List.of("central", "harbor")) {
			JsonObject zone = zone(oldDocument, id);
			zone.remove("id");
			zone.remove("confirmationTicks");
		}

		RailwayNetwork migrated = RailwayNetworkJsonCodec.decode(oldDocument.toString());
		RailwayNetwork again = RailwayNetworkJsonCodec.decode(oldDocument.toString());
		Zone central = migrated.controlPoints().get("central").zone();
		Zone harbor = migrated.controlPoints().get("harbor").zone();

		assertEquals(8, migrated.revision());
		assertEquals("Central", migrated.controlPoints().get("central").name());
		assertEquals(network().controlPoints().get("central").zone().boxes(), central.boxes());
		assertEquals(3, central.confirmationTicks());
		assertEquals(3, harbor.confirmationTicks());
		assertTrue(central.id().matches("zone-[a-f0-9]{32}"));
		assertNotEquals(central.id(), harbor.id(), "separate legacy zones need distinct IDs");
		assertEquals(central.id(), again.controlPoints().get("central").zone().id(),
				"legacy IDs must be deterministic, not random on each read");

		String upgraded = RailwayNetworkJsonCodec.encode(migrated);
		assertEquals(2, JsonParser.parseString(upgraded).getAsJsonObject()
				.get("schemaVersion").getAsInt());
		assertEquals(central, RailwayNetworkJsonCodec.decode(upgraded)
				.controlPoints().get("central").zone());
	}

	@Test
	void malformedOrMissingModernZoneFieldsProduceCheckedErrors() throws Exception {
		JsonObject missingId = asJson(network());
		zone(missingId, "central").remove("id");
		assertInvalid(missingId);

		JsonObject nullId = asJson(network());
		zone(nullId, "central").add("id", JsonNull.INSTANCE);
		assertInvalid(nullId);

		JsonObject missingTicks = asJson(network());
		zone(missingTicks, "central").remove("confirmationTicks");
		assertInvalid(missingTicks);

		JsonObject zeroTicks = asJson(network());
		zone(zeroTicks, "central").addProperty("confirmationTicks", 0);
		assertInvalid(zeroTicks);

		JsonObject badBoxes = asJson(network());
		zone(badBoxes, "central").getAsJsonArray("boxes").get(0).getAsJsonObject()
				.getAsJsonObject("min").addProperty("x", 99);
		assertInvalid(badBoxes);
	}

	@Test
	void legacyMigrationDoesNotSilentlyRepairExplicitlyInvalidValues() throws Exception {
		JsonObject document = asJson(network());
		document.addProperty("schemaVersion", 1);
		zone(document, "central").add("id", JsonNull.INSTANCE);
		zone(document, "central").remove("confirmationTicks");
		assertInvalid(document);
	}

	@Test
	void rejectsUnknownSchemaBeforeConstructingBadZoneObjects() throws Exception {
		JsonObject document = asJson(network());
		document.addProperty("schemaVersion", 99);
		zone(document, "central").add("id", JsonNull.INSTANCE);
		NetworkValidationException thrown = assertThrows(NetworkValidationException.class,
				() -> RailwayNetworkJsonCodec.decode(document.toString()));
		assertTrue(thrown.getMessage().contains("Unsupported Railway GPS schema"));
	}

	@Test
	void invalidEnvelopeAndMalformedJsonDoNotLeakRuntimeExceptions() {
		for (String document : List.of("[]", "{}", "{not valid JSON", "{\"schemaVersion\":2}")) {
			assertThrows(NetworkValidationException.class,
					() -> RailwayNetworkJsonCodec.decode(document));
		}
	}

	@Test
	void currentSchemaRoundTripsWithoutChangingZoneFields() throws Exception {
		RailwayNetwork original = network();
		RailwayNetwork loaded = RailwayNetworkJsonCodec.decode(RailwayNetworkJsonCodec.encode(original));
		assertEquals(original, loaded);
	}

	private static void assertInvalid(JsonObject document) {
		assertThrows(NetworkValidationException.class,
				() -> RailwayNetworkJsonCodec.decode(document.toString()));
	}

	private static JsonObject asJson(RailwayNetwork network) throws NetworkValidationException {
		return JsonParser.parseString(RailwayNetworkJsonCodec.encode(network)).getAsJsonObject();
	}

	private static JsonObject zone(JsonObject document, String id) {
		return document.getAsJsonObject("network").getAsJsonObject("controlPoints")
				.getAsJsonObject(id).getAsJsonObject("zone");
	}

	private static RailwayNetwork network() {
		return new RailwayNetwork(8, Map.of(
				"central", station("central", "Central", 0),
				"harbor", station("harbor", "Harbor", 40)),
				Map.of(), Map.of(), Map.of(), Map.of());
	}

	private static ControlPoint station(String id, String name, int x) {
		return new ControlPoint(id, name, ControlPoint.Kind.STATION,
				new Zone("zone-" + id, "minecraft:overworld",
						List.of(new ZoneBox(new TrackPoint(x, 63, 0),
								new TrackPoint(x + 5, 65, 4))), 1.0, 3),
				Map.of());
	}
}
