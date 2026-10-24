package se.railwaygps.storage;

import org.junit.jupiter.api.Test;
import se.railwaygps.model.Junction;
import se.railwaygps.model.NetworkValidationException;
import se.railwaygps.model.RailwayNetwork;
import se.railwaygps.model.Route;
import se.railwaygps.model.Station;
import se.railwaygps.model.TrackPoint;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NetworkJsonCodecTest {
	@Test
	void roundTripsValidatedSnapshotDeterministically() throws Exception {
		Station station = new Station("station", "Station", "minecraft:overworld", new TrackPoint(0, 64, 0));
		Junction junction = new Junction("junction", "Junction", "minecraft:overworld", new TrackPoint(10, 64, 0), 25);
		Route route = new Route("route", "Route", "station", "junction", "minecraft:overworld", false, 10,
				List.of(station.position(), junction.position()));
		RailwayNetwork network = new RailwayNetwork(
				Map.of(station.id(), station), Map.of(junction.id(), junction), Map.of(route.id(), route));

		String first = NetworkJsonCodec.encode(network);
		RailwayNetwork decoded = NetworkJsonCodec.decode(first);

		assertEquals(first, NetworkJsonCodec.encode(decoded));
		assertEquals(route, decoded.routes().get("route"));
	}

	@Test
	void rejectsIncompleteSnapshot() {
		assertThrows(NetworkValidationException.class, () -> NetworkJsonCodec.decode("{\"stations\":{}}"));
	}
}
