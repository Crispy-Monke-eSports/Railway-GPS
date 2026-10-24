package se.railwaygps.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import se.railwaygps.model.Junction;
import se.railwaygps.model.RailwayNetwork;
import se.railwaygps.model.Route;
import se.railwaygps.model.Station;
import se.railwaygps.model.TrackPoint;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NetworkStorageTest {
	@TempDir
	Path worldRoot;

	@Test
	void roundTripsOneJsonFilePerObject() throws Exception {
		NetworkStorage storage = new NetworkStorage(worldRoot);
		storage.createStation(new Station("central", "Central", "minecraft:overworld", new TrackPoint(0, 64, 0)));
		storage.createJunction(new Junction("crossing", "Crossing", "minecraft:overworld", new TrackPoint(20, 64, 0), 25));
		storage.createRoute(new Route("line", "Line", "central", "crossing", "minecraft:overworld", false, 20,
				List.of(new TrackPoint(0, 64, 0), new TrackPoint(20, 64, 0))));

		RailwayNetwork loaded = storage.load();

		assertEquals(1, loaded.stations().size());
		assertEquals(1, loaded.junctions().size());
		assertEquals(1, loaded.routes().size());
		assertEquals(new TrackPoint(20, 64, 0), loaded.routes().get("line").points().get(1));
		assertTrue(Files.readString(worldRoot.resolve("railwaygps/routes/line.json")).contains("[\n      0,"));
	}

	@Test
	void refusesOverwriteAndUnsafeId() throws Exception {
		NetworkStorage storage = new NetworkStorage(worldRoot);
		Station station = new Station("central", "Central", "minecraft:overworld", new TrackPoint(0, 64, 0));
		storage.createStation(station);

		assertThrows(Exception.class, () -> storage.createStation(station));
		assertThrows(IllegalArgumentException.class, () -> storage.createStation(
				new Station("../escape", "Escape", "minecraft:overworld", new TrackPoint(0, 64, 0))));
	}

	@Test
	void renamesAndRemovesOnlyTheSelectedRoute() throws Exception {
		NetworkStorage storage = storageWithConnectedNodes();
		Route first = route("first");
		Route second = route("second");
		storage.createRoute(first);
		storage.createRoute(second);

		storage.renameRoute(first, "renamed");
		RailwayNetwork renamed = storage.load();
		assertTrue(!renamed.routes().containsKey("first"));
		assertTrue(renamed.routes().containsKey("renamed"));
		assertEquals("Line", renamed.routes().get("renamed").name());

		storage.removeRoute("renamed");
		RailwayNetwork remaining = storage.load();
		assertEquals(java.util.Set.of("second"), remaining.routes().keySet());
	}

	private NetworkStorage storageWithConnectedNodes() throws Exception {
		NetworkStorage storage = new NetworkStorage(worldRoot);
		storage.createStation(new Station("central", "Central", "minecraft:overworld", new TrackPoint(0, 64, 0)));
		storage.createJunction(new Junction("crossing", "Crossing", "minecraft:overworld", new TrackPoint(20, 64, 0), 25));
		return storage;
	}

	private static Route route(String id) {
		return new Route(id, "Line", "central", "crossing", "minecraft:overworld", false, 20,
				List.of(new TrackPoint(0, 64, 0), new TrackPoint(20, 64, 0)));
	}
}
