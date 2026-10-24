package se.railwaygps.model;

import java.util.Map;

public record RailwayNetwork(
		Map<String, Station> stations,
		Map<String, Junction> junctions,
		Map<String, Route> routes
) {
	public static final RailwayNetwork EMPTY = new RailwayNetwork(Map.of(), Map.of(), Map.of());

	public RailwayNetwork {
		stations = Map.copyOf(stations);
		junctions = Map.copyOf(junctions);
		routes = Map.copyOf(routes);
	}

	public boolean containsNode(String id) {
		return stations.containsKey(id) || junctions.containsKey(id);
	}

	public String nodeDimension(String id) {
		Station station = stations.get(id);
		if (station != null) {
			return station.dimension();
		}
		Junction junction = junctions.get(id);
		return junction == null ? null : junction.dimension();
	}

	public TrackPoint nodePosition(String id) {
		Station station = stations.get(id);
		if (station != null) {
			return station.position();
		}
		Junction junction = junctions.get(id);
		return junction == null ? null : junction.position();
	}

	public double nodeRadius(String id) {
		Junction junction = junctions.get(id);
		return junction == null ? 16.0 : junction.radius();
	}

	public String nodeName(String id) {
		Station station = stations.get(id);
		if (station != null) {
			return station.name();
		}
		Junction junction = junctions.get(id);
		return junction == null ? id : junction.name();
	}

	public Map<String, String> nodeInstructions(String id) {
		Station station = stations.get(id);
		if (station != null) {
			return station.instructions();
		}
		Junction junction = junctions.get(id);
		return junction == null ? Map.of() : junction.instructions();
	}
}
