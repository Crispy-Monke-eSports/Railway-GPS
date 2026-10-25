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

class StationDestinationTest {
	@Test void distinguishesDeletedWrongDimensionAndAvailableStationIds() {
		ControlPoint station = new ControlPoint("central", "Central", ControlPoint.Kind.STATION,
				new Zone("minecraft:overworld", List.of(new ZoneBox(
						new TrackPoint(0, 60, 0), new TrackPoint(2, 70, 2))), 1), Map.of());
		RailwayNetwork network = new RailwayNetwork(1, Map.of(station.id(), station), Map.of(), Map.of(), Map.of(), Map.of());

		assertEquals(StationDestination.Status.AVAILABLE,
				StationDestination.status(network, "central", "minecraft:overworld"));
		assertEquals(StationDestination.Status.WRONG_DIMENSION,
				StationDestination.status(network, "central", "minecraft:the_nether"));
		assertEquals(StationDestination.Status.REMOVED,
				StationDestination.status(network, "deleted-id", "minecraft:overworld"));
	}
}
