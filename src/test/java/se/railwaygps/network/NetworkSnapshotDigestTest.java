package se.railwaygps.network;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NetworkSnapshotDigestTest {
	private static final String JSON = "{\"stations\":{},\"junctions\":{},\"routes\":{}}";

	@Test
	void computesKnownSha256Vector() {
		assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
				NetworkSnapshotDigest.of("abc"));
	}

	@Test
	void producesStableLowercaseDigest() {
		String first = NetworkSnapshotDigest.of(JSON);
		String second = NetworkSnapshotDigest.of(JSON);

		assertEquals(first, second);
		assertEquals(64, first.length());
		assertEquals(first.toLowerCase(), first);
	}

	@Test
	void verifiesMatchingDigestRegardlessOfLetterCase() {
		String digest = NetworkSnapshotDigest.of(JSON);

		assertTrue(NetworkSnapshotDigest.verifies(digest, JSON));
		assertTrue(NetworkSnapshotDigest.verifies(digest.toUpperCase(), JSON));
	}

	@Test
	void rejectsTamperedJson() {
		String digest = NetworkSnapshotDigest.of(JSON);

		assertFalse(NetworkSnapshotDigest.verifies(digest, JSON + " "));
		assertFalse(NetworkSnapshotDigest.verifies(digest, "{\"stations\":{}}"));
	}

	@Test
	void rejectsMalformedAdvertisedDigest() {
		assertFalse(NetworkSnapshotDigest.verifies(null, JSON));
		assertFalse(NetworkSnapshotDigest.verifies("", JSON));
		assertFalse(NetworkSnapshotDigest.verifies("not-a-digest", JSON));
		assertFalse(NetworkSnapshotDigest.verifies(NetworkSnapshotDigest.of(JSON).substring(1), JSON));
	}

	@Test
	void rejectsWellFormedDigestOfOtherContent() {
		assertFalse(NetworkSnapshotDigest.verifies(NetworkSnapshotDigest.of("{}"), JSON));
	}
}
