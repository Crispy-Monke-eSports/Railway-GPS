package se.railwaygps.navigation;

import org.junit.jupiter.api.Test;
import se.railwaygps.railway.model.RailwayNetwork;
import se.railwaygps.railway.model.TrackDirection;
import se.railwaygps.railway.model.TrackLabel;
import se.railwaygps.railway.model.TrackRange;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RailLabelResolverTest {
	@Test void showsOnlyHighestPriorityWayAndLocalIntervalAtCurrentPosition() {
		RailwayNetwork network = new RailwayNetwork(1, Map.of(), Map.of(), Map.of(), Map.of(), Map.of(
				"main", label("main", "Riksettan", TrackLabel.Type.WAY, 20, 0, 100),
				"old", label("old", "Gamla vägen", TrackLabel.Type.WAY, 1, 0, 100),
				"bridge", label("bridge", "Centralbron", TrackLabel.Type.BRIDGE, 10, 20, 40),
				"district", label("district", "Centrum", TrackLabel.Type.DISTRICT, 1, 0, 100)));

		assertEquals("Riksettan · Centralbron",
				RailLabelResolver.at(network, "track", TrackDirection.FORWARD, 30).display());
		assertEquals("Riksettan · Centrum",
				RailLabelResolver.at(network, "track", TrackDirection.FORWARD, 60).display());
		assertEquals("", RailLabelResolver.at(network, "track", TrackDirection.REVERSE, 30).display());
	}

	private static TrackLabel label(String id, String name, TrackLabel.Type type, int priority,
			double start, double end) {
		return new TrackLabel(id, name, type, priority, null,
				List.of(new TrackRange("track", start, end, TrackDirection.FORWARD)));
	}
}
