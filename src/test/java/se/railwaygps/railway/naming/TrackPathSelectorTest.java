package se.railwaygps.railway.naming;

import org.junit.jupiter.api.Test;
import se.railwaygps.model.TrackPoint;
import se.railwaygps.railway.model.ControlPoint;
import se.railwaygps.railway.model.PhysicalTrack;
import se.railwaygps.railway.model.RailwayNetwork;
import se.railwaygps.railway.model.TrackDirection;
import se.railwaygps.railway.model.TrackRange;
import se.railwaygps.railway.model.TrackSection;
import se.railwaygps.railway.model.TrackLabel;
import se.railwaygps.railway.model.TrackTransition;
import se.railwaygps.railway.model.TraversalRef;
import se.railwaygps.railway.model.Zone;
import se.railwaygps.railway.model.ZoneBox;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrackPathSelectorTest {
	private static final String DIMENSION = "minecraft:overworld";

	@Test
	void selectsPartialRangesAcrossARegisteredTransition() {
		RailwayNetwork network = lineNetwork(false);

		TrackPathSelector.Selection selection = TrackPathSelector.select(network,
				new TrackPathSelector.Point(DIMENSION, 2.5, 64.5, 0.5),
				new TrackPathSelector.Point(DIMENSION, 18.5, 64.5, 0.5), 2);

		assertTrue(selection.successful());
		List<TrackRange> ranges = selection.candidates().getFirst().ranges();
		assertEquals(List.of("ab", "bc"), ranges.stream().map(TrackRange::trackId).toList());
		assertEquals(2, ranges.getFirst().startDistance(), 0.01);
		assertEquals(8, ranges.getLast().endDistance(), 0.01);
	}

	@Test
	void returnsAlternativesInsteadOfSilentlyChoosingABranch() {
		RailwayNetwork network = lineNetwork(true);

		TrackPathSelector.Selection selection = TrackPathSelector.select(network,
				new TrackPathSelector.Point(DIMENSION, 2.5, 64.5, 0.5),
				new TrackPathSelector.Point(DIMENSION, 19.5, 64.5, 0.5), 3);

		assertTrue(selection.candidates().size() >= 2);
		assertTrue(selection.candidates().stream().anyMatch(candidate ->
				candidate.ranges().stream().anyMatch(range -> range.trackId().equals("bd"))));
	}

	@Test
	void includesASeparatelyRecordedOppositeTrack() {
		PhysicalTrack forward = track("east", "section", "a", "b", 0, 10, Set.of(TrackDirection.FORWARD), "west");
		PhysicalTrack reverse = new PhysicalTrack("west", "section", "b", "a", DIMENSION,
				List.of(point(10, 1), point(0, 1)), 10, Set.of(TrackDirection.FORWARD), "east", PhysicalTrack.Status.VERIFIED);
		RailwayNetwork network = new RailwayNetwork(1, points("a", "b"),
				Map.of("section", new TrackSection("section", "a", "b", Set.of("east", "west"))),
				Map.of("east", forward, "west", reverse), Map.of(), Map.of());

		List<TrackRange> ranges = TrackPathSelector.includePairedTracks(network,
				List.of(new TrackRange("east", 2, 8, TrackDirection.FORWARD)));

		assertEquals(2, ranges.size());
		assertEquals("west", ranges.getLast().trackId());
		assertEquals(2, ranges.getLast().startDistance(), 0.0001);
		assertEquals(8, ranges.getLast().endDistance(), 0.0001);
		assertEquals(TrackDirection.FORWARD, ranges.getLast().direction());
	}

	@Test
	void appliesAConfirmedCandidateAsANewRevisionWithoutChangingTheTrackGraph() throws Exception {
		RailwayNetwork network = lineNetwork(false);
		TrackLabelEditor.Preview preview = TrackLabelEditor.preview(network,
				new TrackPathSelector.Point(DIMENSION, 2.5, 64.5, 0.5),
				new TrackPathSelector.Point(DIMENSION, 18.5, 64.5, 0.5), 2);

		RailwayNetwork updated = TrackLabelEditor.apply(
				network, "riksettan", "Riksettan", TrackLabel.Type.WAY, 10, 0x35B9F3, preview, 0, false);

		assertEquals(network.revision() + 1, updated.revision());
		assertEquals(network.tracks(), updated.tracks());
		assertEquals(List.of("ab", "bc"), updated.labels().get("riksettan").ranges().stream()
				.map(TrackRange::trackId).toList());
	}

	private static RailwayNetwork lineNetwork(boolean branch) {
		Map<String, ControlPoint> controlPoints = new LinkedHashMap<>(points("a", "b", "c"));
		Map<String, TrackSection> sections = new LinkedHashMap<>();
		Map<String, PhysicalTrack> tracks = new LinkedHashMap<>();
		Map<String, TrackTransition> transitions = new LinkedHashMap<>();
		add(sections, tracks, track("ab", "section_ab", "a", "b", 0, 10, Set.of(TrackDirection.FORWARD), null));
		add(sections, tracks, track("bc", "section_bc", "b", "c", 10, 20, Set.of(TrackDirection.FORWARD), null));
		transitions.put("ab_bc", transition("ab_bc", "b", "ab", "bc"));
		if (branch) {
			controlPoints.put("d", controlPoint("d", 10, 10));
			PhysicalTrack bd = new PhysicalTrack("bd", "section_bd", "b", "d", DIMENSION,
					List.of(point(10, 0), point(10, 10)), 10, Set.of(TrackDirection.FORWARD), null, PhysicalTrack.Status.VERIFIED);
			PhysicalTrack dc = new PhysicalTrack("dc", "section_dc", "d", "c", DIMENSION,
					List.of(point(10, 10), point(20, 0)), Math.sqrt(200), Set.of(TrackDirection.FORWARD), null,
					PhysicalTrack.Status.VERIFIED);
			add(sections, tracks, bd);
			add(sections, tracks, dc);
			transitions.put("ab_bd", transition("ab_bd", "b", "ab", "bd"));
			transitions.put("bd_dc", transition("bd_dc", "d", "bd", "dc"));
		}
		return new RailwayNetwork(1, controlPoints, sections, tracks, transitions, Map.of());
	}

	private static void add(Map<String, TrackSection> sections, Map<String, PhysicalTrack> tracks, PhysicalTrack track) {
		tracks.put(track.id(), track);
		sections.put(track.sectionId(), new TrackSection(
				track.sectionId(), track.fromControlPoint(), track.toControlPoint(), Set.of(track.id())));
	}

	private static TrackTransition transition(String id, String at, String incoming, String outgoing) {
		return new TrackTransition(id, at, new TraversalRef(incoming, TrackDirection.FORWARD),
				new TraversalRef(outgoing, TrackDirection.FORWARD), true, null);
	}

	private static PhysicalTrack track(String id, String section, String from, String to, int x1, int x2,
			Set<TrackDirection> directions, String paired) {
		return new PhysicalTrack(id, section, from, to, DIMENSION, List.of(point(x1, 0), point(x2, 0)),
				Math.abs(x2 - x1), directions, paired, PhysicalTrack.Status.VERIFIED);
	}

	private static Map<String, ControlPoint> points(String... ids) {
		Map<String, ControlPoint> result = new LinkedHashMap<>();
		for (int index = 0; index < ids.length; index++) {
			result.put(ids[index], controlPoint(ids[index], index * 10, 0));
		}
		return result;
	}

	private static ControlPoint controlPoint(String id, int x, int z) {
		return new ControlPoint(id, id.toUpperCase(), ControlPoint.Kind.JUNCTION,
				new Zone(DIMENSION, List.of(new ZoneBox(point(x, z), new TrackPoint(x + 1, 66, z + 1))), 1.5), Map.of());
	}

	private static TrackPoint point(int x, int z) {
		return new TrackPoint(x, 64, z);
	}
}
