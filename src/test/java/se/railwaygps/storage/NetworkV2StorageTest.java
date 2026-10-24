package se.railwaygps.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import se.railwaygps.model.TrackPoint;
import se.railwaygps.v2.model.ControlPoint;
import se.railwaygps.v2.model.RailwayNetworkV2;
import se.railwaygps.v2.model.Zone;
import se.railwaygps.v2.model.ZoneBox;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NetworkV2StorageTest {
	@TempDir
	Path world;

	@Test
	void storesOnlyTheNewVersionedDocumentAndIgnoresLegacyDirectories() throws Exception {
		Path oldStations = world.resolve("railwaygps/stations");
		Files.createDirectories(oldStations);
		Files.writeString(oldStations.resolve("legacy.json"), "{broken legacy data}");
		NetworkV2Storage storage = new NetworkV2Storage(world);
		RailwayNetworkV2 network = new RailwayNetworkV2(7,
				Map.of("central", new ControlPoint("central", "Central", ControlPoint.Kind.STATION,
						new Zone("minecraft:overworld", List.of(new ZoneBox(
								new TrackPoint(0, 60, 0), new TrackPoint(10, 70, 10))), 1.5), Map.of())),
				Map.of(), Map.of(), Map.of(), Map.of());

		storage.save(network);
		RailwayNetworkV2 loaded = storage.load();

		assertEquals(7, loaded.revision());
		assertEquals("Central", loaded.controlPoints().get("central").name());
		assertTrue(Files.exists(world.resolve("railwaygps/network-v2.json")));
		assertTrue(Files.exists(oldStations.resolve("legacy.json")));
		assertFalse(Files.readString(storage.file()).contains("legacy"));
	}
}
