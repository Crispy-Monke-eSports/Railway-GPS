package se.railwaygps.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import se.railwaygps.model.TrackPoint;
import se.railwaygps.railway.model.ControlPoint;
import se.railwaygps.railway.model.RailwayNetwork;
import se.railwaygps.railway.model.Zone;
import se.railwaygps.railway.model.ZoneBox;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RailwayNetworkStorageTest {
	@TempDir
	Path world;

	@Test
	void storesTheAuthoritativeDocument() throws Exception {
		RailwayNetworkStorage storage = new RailwayNetworkStorage(world);
		RailwayNetwork network = new RailwayNetwork(7,
				Map.of("central", new ControlPoint("central", "Central", ControlPoint.Kind.STATION,
						new Zone("minecraft:overworld", List.of(new ZoneBox(
								new TrackPoint(0, 60, 0), new TrackPoint(10, 70, 10))), 1.5), Map.of())),
				Map.of(), Map.of(), Map.of(), Map.of());

		storage.save(network);
		RailwayNetwork loaded = storage.load();

		assertEquals(7, loaded.revision());
		assertEquals("Central", loaded.controlPoints().get("central").name());
		assertTrue(Files.exists(world.resolve("railwaygps/network.json")));
	}
}
