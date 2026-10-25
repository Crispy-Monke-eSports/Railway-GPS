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
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
		assertEquals(network.controlPoints().get("central").zone().id(),
				loaded.controlPoints().get("central").zone().id());
		assertEquals(3, loaded.controlPoints().get("central").zone().confirmationTicks());
		assertTrue(Files.exists(world.resolve("railwaygps/network.json")));
	}

	@Test
	void failedCommitPreservesThePreviousDocumentAndRemovesTemporaryFile() throws Exception {
		RailwayNetworkStorage storage = new RailwayNetworkStorage(world);
		storage.save(network(7, "Central"));
		RailwayNetworkStorage failing = new RailwayNetworkStorage(world, (temporary, destination) -> {
			assertTrue(Files.size(temporary) > 0, "candidate must be completely written before commit");
			throw new java.io.IOException("simulated move failure");
		});

		assertThrows(java.io.IOException.class, () -> failing.save(network(8, "Changed")));
		assertEquals(7, storage.load().revision());
		assertEquals("Central", storage.load().controlPoints().get("central").name());
		try (Stream<Path> files = Files.list(world.resolve("railwaygps"))) {
			assertEquals(List.of("network.json"), files.map(path -> path.getFileName().toString()).sorted().toList());
		}
	}

	@Test
	void invalidCandidateNeverTouchesThePersistedRevision() throws Exception {
		RailwayNetworkStorage storage = new RailwayNetworkStorage(world);
		storage.save(network(7, "Central"));
		RailwayNetwork invalid = new RailwayNetwork(8,
				Map.of("central", new ControlPoint("wrong-id", "Changed", ControlPoint.Kind.STATION,
						zone(), Map.of())), Map.of(), Map.of(), Map.of(), Map.of());

		assertThrows(se.railwaygps.model.NetworkValidationException.class, () -> storage.save(invalid));
		assertEquals(7, storage.load().revision());
		assertEquals("Central", storage.load().controlPoints().get("central").name());
	}

	@Test
	void failedLoadCannotOverwriteTheOriginalUntilAValidReload() throws Exception {
		RailwayNetworkStorage storage = new RailwayNetworkStorage(world);
		storage.save(network(7, "Central"));
		Path originalFile = world.resolve("railwaygps/network.json");
		String valid = Files.readString(originalFile);
		String invalid = valid.replace("\"confirmationTicks\": 3", "\"confirmationTicks\": 0");
		assertTrue(!valid.equals(invalid), "the fixture must have a serialized zone");
		Files.writeString(originalFile, invalid);

		assertThrows(se.railwaygps.model.NetworkValidationException.class, storage::load);
		assertThrows(java.io.IOException.class, () -> storage.save(network(8, "Changed")));
		assertEquals(invalid, Files.readString(originalFile),
				"a failed startup must not overwrite the only copy of the broken network");

		// An external repair/restore followed by a successful reload clears the guard.
		Files.writeString(originalFile, valid);
		assertEquals(7, storage.load().revision());
		storage.save(network(8, "Changed"));
		assertEquals(8, storage.load().revision());
	}

	private static RailwayNetwork network(long revision, String name) {
		return new RailwayNetwork(revision,
				Map.of("central", new ControlPoint("central", name, ControlPoint.Kind.STATION, zone(), Map.of())),
				Map.of(), Map.of(), Map.of(), Map.of());
	}

	private static Zone zone() {
		return new Zone("minecraft:overworld", List.of(new ZoneBox(
				new TrackPoint(0, 60, 0), new TrackPoint(10, 70, 10))), 1.5);
	}
}
