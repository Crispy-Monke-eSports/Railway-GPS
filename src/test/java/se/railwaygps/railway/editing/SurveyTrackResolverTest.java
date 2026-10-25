package se.railwaygps.railway.editing;

import org.junit.jupiter.api.Test;
import se.railwaygps.model.TrackPoint;
import se.railwaygps.railway.model.ControlPoint;
import se.railwaygps.railway.model.PhysicalTrack;
import se.railwaygps.railway.model.RailwayNetwork;
import se.railwaygps.railway.model.TrackDirection;
import se.railwaygps.railway.model.TrackSection;
import se.railwaygps.railway.model.Zone;
import se.railwaygps.railway.model.ZoneBox;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SurveyTrackResolverTest {
	@Test
	void anchorsAtAnUnambiguousPointAndFollowsApprovedDirection() {
		RailwayNetwork network = network(track("main", 0, Set.of(TrackDirection.FORWARD)));
		SurveyTrackResolver.Anchor anchor = SurveyTrackResolver.anchor(network, "minecraft:overworld", point(5));

		assertEquals("main", anchor.trackId());
		assertEquals(TrackDirection.FORWARD, SurveyTrackResolver.resolveAnchorTraversal(network, anchor, "b",
				List.of(point(5), point(8), point(10))).direction());
		assertThrows(IllegalArgumentException.class, () -> SurveyTrackResolver.resolveAnchorTraversal(
				network, anchor, "a", List.of(point(5), point(2), point(0))));
	}

	@Test
	void rejectsUnregisteredAndAmbiguousStartingRails() {
		assertThrows(IllegalArgumentException.class, () -> SurveyTrackResolver.anchor(
				RailwayNetwork.EMPTY, "minecraft:overworld", point(5)));
		PhysicalTrack first = track("first", 0, Set.of(TrackDirection.FORWARD));
		PhysicalTrack second = track("second", 0, Set.of(TrackDirection.FORWARD));
		RailwayNetwork network = network(first, second);
		assertThrows(IllegalArgumentException.class, () -> SurveyTrackResolver.anchor(
				network, "minecraft:overworld", point(5)));
	}

	@Test
	void reverseSurveyReusesGeometryAndOnlyAddsObservedDirection() throws Exception {
		RailwayNetwork original = network(track("main", 0, Set.of(TrackDirection.FORWARD)));
		SurveyTrackResolver.ResolvedTraversal resolved = SurveyTrackResolver.resolveOrCreate(original,
				"b", "a", "minecraft:overworld", List.of(point(10), point(5), point(0)));

		assertFalse(resolved.created());
		assertEquals("main", resolved.traversal().trackId());
		assertEquals(TrackDirection.REVERSE, resolved.traversal().direction());
		assertEquals(Set.of(TrackDirection.FORWARD, TrackDirection.REVERSE),
				resolved.network().tracks().get("main").directions());
		assertEquals(1, resolved.network().tracks().size());
	}

	@Test
	void forwardSurveyDoesNotInventReversePermission() throws Exception {
		RailwayNetwork original = network();
		SurveyTrackResolver.ResolvedTraversal resolved = SurveyTrackResolver.resolveOrCreate(original,
				"a", "b", "minecraft:overworld", List.of(point(0), point(5), point(10)));

		assertTrue(resolved.created());
		assertEquals(Set.of(TrackDirection.FORWARD),
				resolved.network().tracks().get(resolved.traversal().trackId()).directions());
		assertEquals(1, resolved.network().tracks().size());
	}

	@Test
	void distinctParallelGeometryCreatesIndependentTrack() throws Exception {
		RailwayNetwork original = network(track("main", 0, Set.of(TrackDirection.FORWARD)));
		SurveyTrackResolver.ResolvedTraversal resolved = SurveyTrackResolver.resolveOrCreate(original,
				"a", "b", "minecraft:overworld",
				List.of(new TrackPoint(0, 64, 2), new TrackPoint(10, 64, 2)));

		assertEquals(2, resolved.network().tracks().size());
		assertNull(resolved.network().tracks().get("main").pairedTrackId());
	}

	private static RailwayNetwork network(PhysicalTrack... tracks) {
		Map<String, PhysicalTrack> trackMap = java.util.Arrays.stream(tracks)
				.collect(java.util.stream.Collectors.toMap(PhysicalTrack::id, track -> track));
		Set<String> ids = trackMap.keySet();
		return new RailwayNetwork(1,
				Map.of("a", control("a", -1), "b", control("b", 10)),
				Map.of("section", new TrackSection("section", "a", "b", ids)),
				trackMap, Map.of(), Map.of());
	}

	private static PhysicalTrack track(String id, int z, Set<TrackDirection> directions) {
		return new PhysicalTrack(id, "section", "a", "b", "minecraft:overworld",
				List.of(new TrackPoint(0, 64, z), new TrackPoint(10, 64, z)), 10,
				directions, null, PhysicalTrack.Status.VERIFIED);
	}

	private static ControlPoint control(String id, int x) {
		return new ControlPoint(id, id, ControlPoint.Kind.JUNCTION,
				new Zone("minecraft:overworld", List.of(new ZoneBox(
						new TrackPoint(x, 63, -1), new TrackPoint(x + 1, 66, 1))), 1), Map.of());
	}

	private static TrackPoint point(int x) { return new TrackPoint(x, 64, 0); }
}
