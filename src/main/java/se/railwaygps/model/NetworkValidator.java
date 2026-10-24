package se.railwaygps.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public final class NetworkValidator {
	private static final Pattern ID_PATTERN = Pattern.compile("[a-z0-9][a-z0-9_-]{0,63}");

	private NetworkValidator() {
	}

	public static boolean isValidId(String id) {
		return id != null && ID_PATTERN.matcher(id).matches();
	}

	public static void validate(RailwayNetwork network) throws NetworkValidationException {
		List<String> errors = new ArrayList<>();
		Set<String> nodeIds = new HashSet<>();

		validateStations(network.stations(), nodeIds, errors);
		validateJunctions(network.junctions(), nodeIds, errors);
		validateRoutes(network, errors);
		validateInstructions(network, errors);

		if (!errors.isEmpty()) {
			throw new NetworkValidationException(errors);
		}
	}

	private static void validateStations(Map<String, Station> stations, Set<String> nodeIds, List<String> errors) {
		stations.forEach((fileId, station) -> {
			if (station == null) {
				errors.add("Station '" + fileId + "' is null");
				return;
			}
			validateIdentity("Station", fileId, station.id(), station.name(), errors);
			validateLocation("Station '" + fileId + "'", station.dimension(), station.position(), errors);
			if (station.id() != null && !nodeIds.add(station.id())) {
				errors.add("Duplicate node id '" + station.id() + "'");
			}
		});
	}

	private static void validateJunctions(Map<String, Junction> junctions, Set<String> nodeIds, List<String> errors) {
		junctions.forEach((fileId, junction) -> {
			if (junction == null) {
				errors.add("Junction '" + fileId + "' is null");
				return;
			}
			validateIdentity("Junction", fileId, junction.id(), junction.name(), errors);
			validateLocation("Junction '" + fileId + "'", junction.dimension(), junction.position(), errors);
			if (!Double.isFinite(junction.radius()) || junction.radius() <= 0) {
				errors.add("Junction '" + fileId + "' must have a positive finite radius");
			}
			if (junction.id() != null && !nodeIds.add(junction.id())) {
				errors.add("Duplicate node id '" + junction.id() + "'");
			}
		});
	}

	private static void validateRoutes(RailwayNetwork network, List<String> errors) {
		network.routes().forEach((fileId, route) -> {
			if (route == null) {
				errors.add("Route '" + fileId + "' is null");
				return;
			}
			validateIdentity("Route", fileId, route.id(), route.name(), errors);
			if (!network.containsNode(route.from())) {
				errors.add("Route '" + fileId + "' refers to missing start node '" + route.from() + "'");
			}
			if (!network.containsNode(route.to())) {
				errors.add("Route '" + fileId + "' refers to missing end node '" + route.to() + "'");
			}
			if (route.from() != null && route.from().equals(route.to())) {
				errors.add("Route '" + fileId + "' must connect two different nodes");
			}
			if (isBlank(route.dimension())) {
				errors.add("Route '" + fileId + "' has no dimension");
			} else {
				validateRouteDimension(network, route, fileId, errors);
			}
			if (!Double.isFinite(route.distance()) || route.distance() <= 0) {
				errors.add("Route '" + fileId + "' must have a positive finite distance");
			}
			if (route.points() == null || route.points().size() < 2 || route.points().stream().anyMatch(point -> point == null)) {
				errors.add("Route '" + fileId + "' must contain at least two valid track points");
			}
		});
	}

	private static void validateRouteDimension(RailwayNetwork network, Route route, String id, List<String> errors) {
		String fromDimension = network.nodeDimension(route.from());
		String toDimension = network.nodeDimension(route.to());
		if (fromDimension != null && !route.dimension().equals(fromDimension)) {
			errors.add("Route '" + id + "' dimension differs from its start node");
		}
		if (toDimension != null && !route.dimension().equals(toDimension)) {
			errors.add("Route '" + id + "' dimension differs from its end node");
		}
	}

	private static void validateInstructions(RailwayNetwork network, List<String> errors) {
		for (String nodeId : network.stations().keySet()) {
			validateNodeInstructions(network, nodeId, network.stations().get(nodeId).instructions(), errors);
		}
		for (String nodeId : network.junctions().keySet()) {
			validateNodeInstructions(network, nodeId, network.junctions().get(nodeId).instructions(), errors);
		}
	}

	private static void validateNodeInstructions(
			RailwayNetwork network, String nodeId, Map<String, String> instructions, List<String> errors) {
		instructions.forEach((key, text) -> {
			TransitionInstructions.Transition transition = TransitionInstructions.parse(key).orElse(null);
			if (transition == null) {
				errors.add("Node '" + nodeId + "' has invalid instruction key '" + key + "'");
				return;
			}
			if (isBlank(text) || text.length() > 256) {
				errors.add("Node '" + nodeId + "' instruction '" + key + "' must contain 1-256 characters");
			}
			if (!TransitionInstructions.START.equals(transition.incomingRoute())) {
				Route incoming = network.routes().get(transition.incomingRoute());
				if (incoming == null) {
					errors.add("Node '" + nodeId + "' instruction refers to missing incoming route '"
						+ transition.incomingRoute() + "'");
				} else if (!canArrive(incoming, nodeId)) {
					errors.add("Route '" + incoming.id() + "' cannot arrive at instruction node '" + nodeId + "'");
				}
			}
			Route outgoing = network.routes().get(transition.outgoingRoute());
			if (outgoing == null) {
				errors.add("Node '" + nodeId + "' instruction refers to missing outgoing route '"
						+ transition.outgoingRoute() + "'");
			} else if (!canDepart(outgoing, nodeId)) {
				errors.add("Route '" + outgoing.id() + "' cannot depart instruction node '" + nodeId + "'");
			}
		});
	}

	public static boolean canArrive(Route route, String nodeId) {
		return nodeId.equals(route.to()) || route.bidirectional() && nodeId.equals(route.from());
	}

	public static boolean canDepart(Route route, String nodeId) {
		return nodeId.equals(route.from()) || route.bidirectional() && nodeId.equals(route.to());
	}

	private static void validateIdentity(String kind, String fileId, String objectId, String name, List<String> errors) {
		if (!isValidId(objectId)) {
			errors.add(kind + " '" + fileId + "' has an invalid id");
		}
		if (!fileId.equals(objectId)) {
			errors.add(kind + " file id '" + fileId + "' does not match object id '" + objectId + "'");
		}
		if (isBlank(name)) {
			errors.add(kind + " '" + fileId + "' has no display name");
		}
	}

	private static void validateLocation(String label, String dimension, TrackPoint position, List<String> errors) {
		if (isBlank(dimension)) {
			errors.add(label + " has no dimension");
		}
		if (position == null) {
			errors.add(label + " has no position");
		}
	}

	private static boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}
