package se.railwaygps.integration.journeymap;

import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.client.display.DisplayType;
import journeymap.api.v2.client.display.PolygonOverlay;
import journeymap.api.v2.client.model.MapPolygon;
import journeymap.api.v2.client.model.ShapeProperties;
import journeymap.api.v2.common.waypoint.Waypoint;
import journeymap.api.v2.common.waypoint.WaypointFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import se.railwaygps.RailwayGps;
import se.railwaygps.model.Junction;
import se.railwaygps.model.RailwayNetwork;
import se.railwaygps.model.Route;
import se.railwaygps.model.Station;
import se.railwaygps.model.TrackPoint;
import se.railwaygps.navigation.RoutePlan;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

final class JourneyMapOverlayController {
	private static final int STATION_COLOR = 0x35C98A;
	private static final int JUNCTION_COLOR = 0xE45757;
	private static final int TRACK_COLOR = 0x89949D;
	private static final int ACTIVE_ROUTE_COLOR = 0x35B9F3;
	private static final double HALF_TRACK_WIDTH = 1.25;

	private final IClientAPI api;
	private final List<PolygonOverlay> routeOverlays = new ArrayList<>();

	JourneyMapOverlayController(IClientAPI api) {
		this.api = api;
	}

	void refresh(RailwayNetwork network, Optional<RoutePlan> activePlan, int currentLeg) {
		clear();
		if (network == RailwayNetwork.EMPTY) {
			return;
		}

		for (Station station : network.stations().values()) {
			addWaypoint(station.id(), station.name(), "Railway GPS station", station.dimension(), station.position(), STATION_COLOR);
		}
		for (Junction junction : network.junctions().values()) {
			addWaypoint(junction.id(), junction.name(), "Railway GPS junction", junction.dimension(), junction.position(), JUNCTION_COLOR);
		}

		if (!api.playerAccepts(RailwayGps.MOD_ID, DisplayType.Polygon)) {
			return;
		}
		Set<String> highlightedRoutes = remainingRouteIds(activePlan, currentLeg);
		for (Route route : network.routes().values()) {
			addRoute(route, highlightedRoutes.contains(route.id()));
		}
	}

	private void clear() {
		api.removeAllWaypoints(RailwayGps.MOD_ID);
		for (PolygonOverlay overlay : routeOverlays) {
			api.remove(overlay);
		}
		routeOverlays.clear();
	}

	private void addWaypoint(String id, String name, String description, String dimension, TrackPoint point, int color) {
		try {
			Waypoint waypoint = WaypointFactory.createWaypoint(
					RailwayGps.MOD_ID,
					new BlockPos(point.x(), point.y(), point.z()),
					name,
					dimension,
					false);
			waypoint.setColor(color);
			waypoint.setDescription(description + " (" + id + ")");
			waypoint.setShowDeviation(false);
			api.addWaypoint(RailwayGps.MOD_ID, waypoint);
		} catch (RuntimeException exception) {
			RailwayGps.LOGGER.warn("Could not add JourneyMap waypoint '{}'", id, exception);
		}
	}

	private void addRoute(Route route, boolean highlighted) {
		List<TrackPoint> points = route.points();
		for (int index = 1; index < points.size(); index++) {
			MapPolygon polygon = segmentPolygon(points.get(index - 1), points.get(index));
			if (polygon == null) {
				continue;
			}
			int color = highlighted ? ACTIVE_ROUTE_COLOR : TRACK_COLOR;
			ShapeProperties shape = new ShapeProperties()
					.setStrokeWidth(highlighted ? 2.5f : 1.25f)
					.setStrokeColor(color)
					.setStrokeOpacity(highlighted ? 1.0f : 0.75f)
					.setFillColor(color)
					.setFillOpacity(highlighted ? 0.55f : 0.25f);
			PolygonOverlay overlay = new PolygonOverlay(
					RailwayGps.MOD_ID, dimension(route.dimension()), shape, polygon);
			overlay.setOverlayGroupName(highlighted ? "Railway GPS active route" : "Railway GPS tracks")
					.setTitle(route.name() + " (" + route.id() + ")")
					.setDisplayOrder(highlighted ? 2000 : 1000);
			try {
				api.show(overlay);
				routeOverlays.add(overlay);
			} catch (Exception exception) {
				RailwayGps.LOGGER.warn("Could not show JourneyMap overlay for route '{}'", route.id(), exception);
			}
		}
	}

	private static MapPolygon segmentPolygon(TrackPoint first, TrackPoint second) {
		double dx = second.x() - first.x();
		double dz = second.z() - first.z();
		double length = Math.hypot(dx, dz);
		if (length == 0) {
			return null;
		}
		double nx = -dz / length * HALF_TRACK_WIDTH;
		double nz = dx / length * HALF_TRACK_WIDTH;
		int y = Math.max(first.y(), second.y());
		return new MapPolygon(
				block(first.x() + nx, y, first.z() + nz),
				block(first.x() - nx, y, first.z() - nz),
				block(second.x() - nx, y, second.z() - nz),
				block(second.x() + nx, y, second.z() + nz));
	}

	private static BlockPos block(double x, int y, double z) {
		return new BlockPos((int) Math.round(x), y, (int) Math.round(z));
	}

	private static ResourceKey<Level> dimension(String id) {
		return ResourceKey.create(Registries.DIMENSION, Identifier.parse(id));
	}

	private static Set<String> remainingRouteIds(Optional<RoutePlan> plan, int currentLeg) {
		Set<String> ids = new HashSet<>();
		plan.ifPresent(value -> {
			for (int index = Math.max(0, currentLeg); index < value.legs().size(); index++) {
				ids.add(value.legs().get(index).route().id());
			}
		});
		return ids;
	}
}
