package se.railwaygps.railway.model;

import org.junit.jupiter.api.Test;
import se.railwaygps.model.TrackPoint;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class PhysicalTopologyTest {
	@Test
	void bidirectionalSharedRailHasTwoPassagesAndOnlyTwoPorts() {
		PhysicalTrack track = track("single", 0, Set.of(TrackDirection.FORWARD, TrackDirection.REVERSE));
		PhysicalTopology topology = PhysicalTopology.from(network(track));

		assertEquals(2, topology.ports().size());
		assertEquals(2, topology.passages().size());
		assertEquals("port-single-from", topology.ports().getFirst().id());
		assertEquals("a", topology.ports().getFirst().controlPointId());
		assertEquals(topology.passages().getFirst().startPortId(), topology.passages().getLast().endPortId());
		assertEquals(1, network(track).tracks().size());
	}

	@Test
	void parallelRailsKeepIndependentPortsGeometryAndPassages() {
		PhysicalTrack first = track("east", 0, Set.of(TrackDirection.FORWARD));
		PhysicalTrack second = track("west", 2, Set.of(TrackDirection.REVERSE));
		PhysicalTopology topology = PhysicalTopology.from(network(first, second));

		assertEquals(4, topology.ports().size());
		assertEquals(2, topology.passages().size());
		assertNotEquals(topology.ports().getFirst().position(), topology.ports().get(2).position());
	}

	private static RailwayNetwork network(PhysicalTrack... tracks) {
		Map<String, PhysicalTrack> map = java.util.Arrays.stream(tracks)
				.collect(java.util.stream.Collectors.toMap(PhysicalTrack::id, track -> track));
		return new RailwayNetwork(1, Map.of(), Map.of(), map, Map.of(), Map.of());
	}

	private static PhysicalTrack track(String id, int z, Set<TrackDirection> directions) {
		return new PhysicalTrack(id, "section", "a", "b", "minecraft:overworld",
				List.of(new TrackPoint(0, 64, z), new TrackPoint(10, 64, z)), 10,
				directions, null, PhysicalTrack.Status.VERIFIED);
	}
}
