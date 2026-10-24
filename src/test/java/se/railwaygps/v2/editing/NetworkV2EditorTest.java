package se.railwaygps.v2.editing;

import org.junit.jupiter.api.Test;
import se.railwaygps.model.TrackPoint;
import se.railwaygps.v2.model.ControlPoint;
import se.railwaygps.v2.model.RailwayNetworkV2;
import se.railwaygps.v2.model.TrackDirection;
import se.railwaygps.v2.model.TraversalRef;
import se.railwaygps.v2.model.Zone;
import se.railwaygps.v2.model.ZoneBox;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NetworkV2EditorTest {
	@Test
	void recordsOppositeRailsInOneSectionAndPairsThem() throws Exception {
		RailwayNetworkV2 network = withControlPoints();
		network = NetworkV2Editor.addRecordedTrack(network, "eastbound", "west", "east", "minecraft:overworld",
				List.of(point(2, 64, 0), point(8, 64, 0)));
		network = NetworkV2Editor.addRecordedTrack(network, "westbound", "east", "west", "minecraft:overworld",
				List.of(point(8, 64, 2), point(2, 64, 2)));
		network = NetworkV2Editor.pairTracks(network, "eastbound", "westbound");

		assertEquals(1, network.sections().size());
		assertEquals(2, network.sections().values().iterator().next().trackIds().size());
		assertEquals("westbound", network.tracks().get("eastbound").pairedTrackId());
		assertEquals("eastbound", network.tracks().get("westbound").pairedTrackId());
		assertEquals(5, network.revision());
	}

	@Test
	void transitionMustActuallyMeetAtControlPoint() throws Exception {
		RailwayNetworkV2 network = withThreeControlPoints();
		network = NetworkV2Editor.addRecordedTrack(network, "into_center", "west", "center", "minecraft:overworld",
				List.of(point(2, 64, 0), point(9, 64, 0)));
		network = NetworkV2Editor.addRecordedTrack(network, "out_of_center", "center", "east", "minecraft:overworld",
				List.of(point(11, 64, 0), point(18, 64, 0)));

		RailwayNetworkV2 updated = NetworkV2Editor.addTransition(network, "through_center", "center",
				new TraversalRef("into_center", TrackDirection.FORWARD),
				new TraversalRef("out_of_center", TrackDirection.FORWARD), "Fortsätt framåt");
		assertEquals("Fortsätt framåt", updated.transitions().get("through_center").instruction());

		RailwayNetworkV2 finalNetwork = network;
		assertThrows(Exception.class, () -> NetworkV2Editor.addTransition(finalNetwork, "wrong", "west",
				new TraversalRef("into_center", TrackDirection.FORWARD),
				new TraversalRef("out_of_center", TrackDirection.FORWARD), null));
	}

	private static RailwayNetworkV2 withControlPoints() throws Exception {
		RailwayNetworkV2 network = RailwayNetworkV2.EMPTY;
		network = NetworkV2Editor.addControlPoint(network, "west", "West", ControlPoint.Kind.JUNCTION, zone(0));
		return NetworkV2Editor.addControlPoint(network, "east", "East", ControlPoint.Kind.JUNCTION, zone(10));
	}

	private static RailwayNetworkV2 withThreeControlPoints() throws Exception {
		RailwayNetworkV2 network = RailwayNetworkV2.EMPTY;
		network = NetworkV2Editor.addControlPoint(network, "west", "West", ControlPoint.Kind.JUNCTION, zone(0));
		network = NetworkV2Editor.addControlPoint(network, "center", "Center", ControlPoint.Kind.JUNCTION, zone(10));
		return NetworkV2Editor.addControlPoint(network, "east", "East", ControlPoint.Kind.JUNCTION, zone(20));
	}

	private static Zone zone(int x) {
		return new Zone("minecraft:overworld", List.of(new ZoneBox(point(x, 63, -1), point(x + 1, 66, 1))), 1);
	}

	private static TrackPoint point(int x, int y, int z) {
		return new TrackPoint(x, y, z);
	}
}
