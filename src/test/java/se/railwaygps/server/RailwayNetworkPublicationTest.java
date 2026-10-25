package se.railwaygps.server;

import org.junit.jupiter.api.Test;
import se.railwaygps.railway.model.RailwayNetwork;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RailwayNetworkPublicationTest {
	@Test
	void validCandidateBecomesActiveAndSynchronizesImmediatelyAfterPersistence() throws Exception {
		List<String> events = new ArrayList<>();
		RailwayNetwork initial = revision(1);
		RailwayNetwork candidate = revision(2);
		RailwayNetworkPublication publication = new RailwayNetworkPublication(initial,
				network -> events.add("saved:" + network.revision()),
				network -> events.add("synced:" + network.revision()));

		publication.publish(candidate);

		assertSame(candidate, publication.active());
		assertEquals(List.of("saved:2", "synced:2"), events);
	}

	@Test
	void failedPersistenceLeavesOldRevisionActiveAndUnsynchronized() {
		List<RailwayNetwork> synchronizedNetworks = new ArrayList<>();
		RailwayNetwork initial = revision(1);
		RailwayNetworkPublication publication = new RailwayNetworkPublication(initial,
				network -> { throw new IOException("disk full"); }, synchronizedNetworks::add);

		assertThrows(IOException.class, () -> publication.publish(revision(2)));
		assertSame(initial, publication.active());
		assertEquals(List.of(), synchronizedNetworks);
	}

	private static RailwayNetwork revision(long revision) {
		return new RailwayNetwork(revision, java.util.Map.of(), java.util.Map.of(), java.util.Map.of(),
				java.util.Map.of(), java.util.Map.of());
	}
}
