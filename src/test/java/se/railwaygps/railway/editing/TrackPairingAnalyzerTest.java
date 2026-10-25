package se.railwaygps.railway.editing;

import org.junit.jupiter.api.Test;
import se.railwaygps.model.TrackPoint;
import se.railwaygps.railway.model.ControlPoint;
import se.railwaygps.railway.model.RailwayNetwork;
import se.railwaygps.railway.model.Zone;
import se.railwaygps.railway.model.ZoneBox;
import se.railwaygps.storage.RailwayNetworkJsonCodec;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrackPairingAnalyzerTest {
	@Test
	void proposesAConsistentlyParallelOppositeRail() throws Exception {
		RailwayNetwork network = tracks(
				track("eastbound", "a", "b", point(0, 64, 0), point(20, 64, 0)),
				track("westbound", "b", "a", point(20, 64, 2), point(0, 64, 2)));

		TrackPairingAnalyzer.Proposal proposal = TrackPairingAnalyzer.proposals(network).getFirst();
		assertEquals(2.0, proposal.averageSpacing(), 0.01);
		assertTrue(proposal.alignment() > 0.99);
		assertFalse(proposal.ambiguous());
	}

	@Test
	void marksMultipleGeometricCandidatesAsAmbiguous() throws Exception {
		RailwayNetwork network = tracks(
				track("eastbound", "a", "b", point(0, 64, 0), point(20, 64, 0)),
				track("westbound_1", "b", "a", point(20, 64, 2), point(0, 64, 2)),
				track("westbound_2", "b", "a", point(20, 64, 4), point(0, 64, 4)));

		assertEquals(2, TrackPairingAnalyzer.proposals(network).size());
		assertTrue(TrackPairingAnalyzer.proposals(network).stream().allMatch(TrackPairingAnalyzer.Proposal::ambiguous));
	}

	@Test
	void rejectsCrossingStackedAndDuplicateGeometry() throws Exception {
		RailwayNetwork base = tracks(track("eastbound", "a", "b", point(0, 64, 0), point(20, 64, 0)));
		RailwayNetwork crossing = add(base, track("crossing", "b", "a",
				point(20, 64, 2), point(10, 64, 12), point(0, 64, 2)));
		RailwayNetwork stacked = add(base, track("stacked", "b", "a", point(20, 67, 0), point(0, 67, 0)));
		RailwayNetwork duplicate = add(base, track("duplicate", "b", "a", point(20, 64, 0), point(0, 64, 0)));

		assertTrue(TrackPairingAnalyzer.proposals(crossing).isEmpty());
		assertTrue(TrackPairingAnalyzer.proposals(stacked).isEmpty());
		assertTrue(TrackPairingAnalyzer.proposals(duplicate).isEmpty());
		assertThrows(IllegalArgumentException.class,
				() -> RailwayNetworkEditor.pairTracks(stacked, "eastbound", "stacked"));
	}

	@Test
	void explicitRejectionIsPersistedAndSuppressesTheProposal() throws Exception {
		RailwayNetwork network = tracks(
				track("eastbound", "a", "b", point(0, 64, 0), point(20, 64, 0)),
				track("westbound", "b", "a", point(20, 64, 2), point(0, 64, 2)));
		network = RailwayNetworkEditor.rejectTrackPairing(network, "eastbound", "westbound");
		RailwayNetwork restored = RailwayNetworkJsonCodec.decode(RailwayNetworkJsonCodec.encode(network));

		assertEquals(1, restored.rejectedTrackPairings().size());
		assertTrue(TrackPairingAnalyzer.proposals(restored).isEmpty());
		assertEquals(null, restored.tracks().get("eastbound").pairedTrackId());
	}

	private static RailwayNetwork tracks(RecordedTrack... tracks) throws Exception {
		RailwayNetwork network = RailwayNetwork.EMPTY;
		network = RailwayNetworkEditor.addControlPoint(network, "a", "A", ControlPoint.Kind.JUNCTION, zone(0));
		network = RailwayNetworkEditor.addControlPoint(network, "b", "B", ControlPoint.Kind.JUNCTION, zone(20));
		for (RecordedTrack track : tracks) network = add(network, track);
		return network;
	}

	private static RailwayNetwork add(RailwayNetwork network, RecordedTrack track) throws Exception {
		return RailwayNetworkEditor.addRecordedTrack(network, track.id, track.from, track.to,
				"minecraft:overworld", track.points);
	}

	private static RecordedTrack track(String id, String from, String to, TrackPoint... points) {
		return new RecordedTrack(id, from, to, List.of(points));
	}

	private static Zone zone(int x) {
		return new Zone("minecraft:overworld", List.of(new ZoneBox(point(x - 2, 62, -5), point(x + 2, 68, 5))), 1);
	}

	private static TrackPoint point(int x, int y, int z) {
		return new TrackPoint(x, y, z);
	}

	private record RecordedTrack(String id, String from, String to, List<TrackPoint> points) { }
}
