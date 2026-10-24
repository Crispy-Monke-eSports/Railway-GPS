package se.railwaygps.navigation;

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

class StartInstructionResolverTest {
	@Test
	void treatsCartAlreadyOnOutgoingRailAsStraight() {
		RailwayNetwork network = network();
		PhysicalRouteLeg outgoing = new PhysicalRouteLeg(network.tracks().get("out"), TrackDirection.FORWARD);

		assertEquals(PhysicalInstructionResolver.Direction.STRAIGHT,
				StartInstructionResolver.resolve(network, outgoing, 0.5, 0.5, -2.5).direction());
	}

	@Test
	void derivesTurnFromIncomingRailGeometry() {
		RailwayNetwork network = network();
		PhysicalRouteLeg outgoing = new PhysicalRouteLeg(network.tracks().get("out"), TrackDirection.FORWARD);

		assertEquals(PhysicalInstructionResolver.Direction.RIGHT,
				StartInstructionResolver.resolve(network, outgoing, 3.5, 0.5, 0.5).direction());
	}

	@Test
	void returnsUnknownAwayFromRegisteredRails() {
		RailwayNetwork network = network();
		PhysicalRouteLeg outgoing = new PhysicalRouteLeg(network.tracks().get("out"), TrackDirection.FORWARD);

		assertEquals(PhysicalInstructionResolver.Direction.UNKNOWN,
				StartInstructionResolver.resolve(network, outgoing, 50, 0, 50).direction());
	}

	private static RailwayNetwork network() {
		Zone zone = new Zone("minecraft:overworld", List.of(new ZoneBox(
				new TrackPoint(-4, -2, -4), new TrackPoint(4, 2, 4))), 1);
		Map<String, ControlPoint> points = Map.of(
				"west", new ControlPoint("west", "West", ControlPoint.Kind.JUNCTION, zone, Map.of()),
				"center", new ControlPoint("center", "Center", ControlPoint.Kind.JUNCTION, zone, Map.of()),
				"north", new ControlPoint("north", "North", ControlPoint.Kind.JUNCTION, zone, Map.of()));
		PhysicalTrack incoming = track("in", "s1", "west", "center",
				List.of(new TrackPoint(10, 0, 0), new TrackPoint(0, 0, 0)));
		PhysicalTrack outgoing = track("out", "s2", "center", "north",
				List.of(new TrackPoint(0, 0, 0), new TrackPoint(0, 0, -10)));
		return new RailwayNetwork(1, points,
				Map.of("s1", new TrackSection("s1", "west", "center", Set.of("in")),
						"s2", new TrackSection("s2", "center", "north", Set.of("out"))),
				Map.of("in", incoming, "out", outgoing), Map.of(), Map.of());
	}

	private static PhysicalTrack track(String id, String section, String from, String to, List<TrackPoint> points) {
		return new PhysicalTrack(id, section, from, to, "minecraft:overworld", points, 10,
				Set.of(TrackDirection.FORWARD), null, PhysicalTrack.Status.VERIFIED);
	}
}
