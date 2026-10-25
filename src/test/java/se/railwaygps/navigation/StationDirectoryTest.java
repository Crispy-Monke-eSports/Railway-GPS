package se.railwaygps.navigation;

import org.junit.jupiter.api.Test;
import se.railwaygps.model.TrackPoint;
import se.railwaygps.railway.model.ControlPoint;
import se.railwaygps.railway.model.RailwayNetwork;
import se.railwaygps.railway.model.Zone;
import se.railwaygps.railway.model.ZoneBox;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StationDirectoryTest {
	@Test
	void exposesOnlyAlphabeticalStationsInTheCurrentDimension() {
		RailwayNetwork network = new RailwayNetwork(1, Map.of(
				"z", point("z", "Östra", ControlPoint.Kind.STATION, "minecraft:overworld"),
				"a", point("a", "Central", ControlPoint.Kind.STATION, "minecraft:overworld"),
				"signal", point("signal", "Signal", ControlPoint.Kind.JUNCTION, "minecraft:overworld"),
				"nether", point("nether", "Nether", ControlPoint.Kind.STATION, "minecraft:the_nether")),
				Map.of(), Map.of(), Map.of(), Map.of());

		assertEquals(List.of("Central", "Östra"), StationDirectory.inDimension(network, "minecraft:overworld")
				.stream().map(ControlPoint::name).toList());
	}

	private static ControlPoint point(String id, String name, ControlPoint.Kind kind, String dimension) {
		return new ControlPoint(id, name, kind, new Zone(dimension,
				List.of(new ZoneBox(new TrackPoint(0, 60, 0), new TrackPoint(2, 65, 2))), 1), Map.of());
	}
}
