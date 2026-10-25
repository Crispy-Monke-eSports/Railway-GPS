package se.railwaygps.railway.editing;

import org.junit.jupiter.api.Test;
import se.railwaygps.model.TrackPoint;
import se.railwaygps.railway.model.ControlPoint;
import se.railwaygps.railway.model.RailwayNetwork;
import se.railwaygps.railway.model.TrackDirection;
import se.railwaygps.railway.model.TraversalRef;
import se.railwaygps.railway.model.Zone;
import se.railwaygps.railway.model.ZoneBox;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RailwayNetworkEditorTest {
	@Test
	void generatesOpaqueTrackIdentity() throws Exception {
		RailwayNetworkEditor.AddedTrack added = RailwayNetworkEditor.addRecordedTrack(withControlPoints(),
				"west", "east", "minecraft:overworld", List.of(point(2, 64, 0), point(8, 64, 0)));

		assertTrue(added.trackId().startsWith("track-"));
		assertTrue(added.network().tracks().containsKey(added.trackId()));
	}

	@Test
	void leavesAnUnambiguousOppositeTrackUnpairedUntilAdminApproval() throws Exception {
		RailwayNetworkEditor.AddedTrack eastbound = RailwayNetworkEditor.addRecordedTrack(withControlPoints(),
				"west", "east", "minecraft:overworld", List.of(point(2, 64, 0), point(8, 64, 0)));
		RailwayNetworkEditor.AddedTrack westbound = RailwayNetworkEditor.addRecordedTrack(eastbound.network(),
				"east", "west", "minecraft:overworld", List.of(point(8, 64, 2), point(2, 64, 2)));

		assertEquals(null, westbound.network().tracks().get(eastbound.trackId()).pairedTrackId());
		assertEquals(null, westbound.network().tracks().get(westbound.trackId()).pairedTrackId());
	}

	@Test
	void recordsOppositeRailsInOneSectionAndPairsThem() throws Exception {
		RailwayNetwork network = withControlPoints();
		network = RailwayNetworkEditor.addRecordedTrack(network, "eastbound", "west", "east", "minecraft:overworld",
				List.of(point(2, 64, 0), point(8, 64, 0)));
		network = RailwayNetworkEditor.addRecordedTrack(network, "westbound", "east", "west", "minecraft:overworld",
				List.of(point(8, 64, 2), point(2, 64, 2)));
		network = RailwayNetworkEditor.pairTracks(network, "eastbound", "westbound");

		assertEquals(1, network.sections().size());
		assertEquals(2, network.sections().values().iterator().next().trackIds().size());
		assertEquals("westbound", network.tracks().get("eastbound").pairedTrackId());
		assertEquals("eastbound", network.tracks().get("westbound").pairedTrackId());
		assertEquals(5, network.revision());
	}

	@Test
	void transitionMustActuallyMeetAtControlPoint() throws Exception {
		RailwayNetwork network = withThreeControlPoints();
		network = RailwayNetworkEditor.addRecordedTrack(network, "into_center", "west", "center", "minecraft:overworld",
				List.of(point(2, 64, 0), point(9, 64, 0)));
		network = RailwayNetworkEditor.addRecordedTrack(network, "out_of_center", "center", "east", "minecraft:overworld",
				List.of(point(11, 64, 0), point(18, 64, 0)));

		RailwayNetwork updated = RailwayNetworkEditor.addTransition(network, "through_center", "center",
				new TraversalRef("into_center", TrackDirection.FORWARD),
				new TraversalRef("out_of_center", TrackDirection.FORWARD), "Fortsätt framåt");
		assertEquals("Fortsätt framåt", updated.transitions().get("through_center").instruction());

		RailwayNetwork finalNetwork = network;
		assertThrows(Exception.class, () -> RailwayNetworkEditor.addTransition(finalNetwork, "wrong", "west",
				new TraversalRef("into_center", TrackDirection.FORWARD),
				new TraversalRef("out_of_center", TrackDirection.FORWARD), null));
	}

	@Test
	void togglesOneRedlightPassageWithoutLosingItsVerifiedIdentityOrInstruction() throws Exception {
		RailwayNetwork network = withThreeControlPoints();
		network = RailwayNetworkEditor.addRecordedTrack(network, "into_center", "west", "center", "minecraft:overworld",
				List.of(point(2, 64, 0), point(9, 64, 0)));
		network = RailwayNetworkEditor.addRecordedTrack(network, "out_of_center", "center", "east", "minecraft:overworld",
				List.of(point(11, 64, 0), point(18, 64, 0)));
		network = RailwayNetworkEditor.addTransition(network, "through_center", "center",
				new TraversalRef("into_center", TrackDirection.FORWARD),
				new TraversalRef("out_of_center", TrackDirection.FORWARD), "Välj knappen för Central");

		RailwayNetwork disabled = RailwayNetworkEditor.setTransitionEnabled(network, "through_center", false);
		RailwayNetwork enabled = RailwayNetworkEditor.setTransitionEnabled(disabled, "through_center", true);
		RailwayNetwork edited = RailwayNetworkEditor.setTransitionInstruction(enabled, "through_center",
				"  Välj knappen för Martyrön!  ");

		assertEquals("Välj knappen för Central", disabled.transitions().get("through_center").instruction());
		assertEquals(network.transitions().get("through_center").incoming(),
				disabled.transitions().get("through_center").incoming());
		assertTrue(!disabled.transitions().get("through_center").enabled());
		assertTrue(enabled.transitions().get("through_center").enabled());
		assertEquals("  Välj knappen för Martyrön!  ", edited.transitions().get("through_center").instruction());
	}

	@Test
	void verifiesReverseTravelOnTheSameGeometryWithoutCreatingOrPairingTracks() throws Exception {
		RailwayNetwork network = RailwayNetworkEditor.addRecordedTrack(withControlPoints(), "single",
				"west", "east", "minecraft:overworld", List.of(point(2, 64, 0), point(8, 64, 0)));
		RailwayNetwork updated = RailwayNetworkEditor.allowTrackDirection(network, "single", TrackDirection.REVERSE);

		assertEquals(1, updated.tracks().size());
		assertEquals(java.util.Set.of(TrackDirection.FORWARD, TrackDirection.REVERSE),
				updated.tracks().get("single").directions());
		assertEquals(null, updated.tracks().get("single").pairedTrackId());
	}

	@Test
	void overlappingControlZoneRequiresExplicitAcknowledgementInTheSameRevision() throws Exception {
		RailwayNetwork network = RailwayNetworkEditor.addControlPoint(RailwayNetwork.EMPTY,
				"first", "First", ControlPoint.Kind.STATION, namedZone("zone-first", 0));
		RailwayNetwork before = network;

		assertThrows(Exception.class, () -> RailwayNetworkEditor.addControlPoint(before,
				"second", "Second", ControlPoint.Kind.STATION, namedZone("zone-second", 0)));
		RailwayNetwork acknowledged = RailwayNetworkEditor.addControlPoint(before,
				"second", "Second", ControlPoint.Kind.STATION, namedZone("zone-second", 0), true);

		assertEquals(1, acknowledged.acknowledgedZoneOverlaps().size());
		assertTrue(se.railwaygps.railway.model.RailwayNetworkValidator.inspect(acknowledged).valid());
		assertTrue(se.railwaygps.railway.model.RailwayNetworkValidator.inspect(acknowledged).warnings().stream()
				.anyMatch(warning -> warning.contains("acknowledged")));
	}

	private static RailwayNetwork withControlPoints() throws Exception {
		RailwayNetwork network = RailwayNetwork.EMPTY;
		network = RailwayNetworkEditor.addControlPoint(network, "west", "West", ControlPoint.Kind.JUNCTION, zone(0));
		return RailwayNetworkEditor.addControlPoint(network, "east", "East", ControlPoint.Kind.JUNCTION, zone(10));
	}

	private static RailwayNetwork withThreeControlPoints() throws Exception {
		RailwayNetwork network = RailwayNetwork.EMPTY;
		network = RailwayNetworkEditor.addControlPoint(network, "west", "West", ControlPoint.Kind.JUNCTION, zone(0));
		network = RailwayNetworkEditor.addControlPoint(network, "center", "Center", ControlPoint.Kind.REDLIGHT, zone(10));
		return RailwayNetworkEditor.addControlPoint(network, "east", "East", ControlPoint.Kind.JUNCTION, zone(20));
	}

	private static Zone zone(int x) {
		return new Zone("minecraft:overworld", List.of(new ZoneBox(point(x, 63, -1), point(x + 1, 66, 1))), 1);
	}

	private static Zone namedZone(String id, int x) {
		return new Zone(id, "minecraft:overworld",
				List.of(new ZoneBox(point(x, 63, -1), point(x + 1, 66, 1))), 1, 3);
	}

	private static TrackPoint point(int x, int y, int z) {
		return new TrackPoint(x, y, z);
	}
}
