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
import se.railwaygps.model.TrackPoint;
import se.railwaygps.navigation.PhysicalRoutePlan;
import se.railwaygps.railway.model.ControlPoint;
import se.railwaygps.railway.model.PhysicalTrack;
import se.railwaygps.railway.model.RailwayNetwork;
import se.railwaygps.railway.model.TrackLabel;
import se.railwaygps.railway.model.ZoneBox;

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
	private static final int PHYSICAL_TRACK_COLOR = 0x4DB7E5;
	private static final int CONTROL_ZONE_COLOR = 0xF2C94C;
	private static final double HALF_TRACK_WIDTH = 1.25;

	private final IClientAPI api;
	private final List<PolygonOverlay> overlays = new ArrayList<>();

	JourneyMapOverlayController(IClientAPI api) {
		this.api = api;
	}

	void refresh(RailwayNetwork network, Optional<PhysicalRoutePlan> activePlan, int currentLeg) {
		clear();
		for (ControlPoint point : network.controlPoints().values()) {
			addControlPoint(point);
		}

		if (!api.playerAccepts(RailwayGps.MOD_ID, DisplayType.Polygon)) {
			return;
		}
		Set<String> highlightedTracks = remainingTrackIds(activePlan, currentLeg);
		for (PhysicalTrack track : network.tracks().values()) {
			addPhysicalTrack(track, network, highlightedTracks.contains(track.id()));
		}
		for (ControlPoint point : network.controlPoints().values()) {
			for (ZoneBox box : point.zone().boxes()) {
				addZoneBox(point, box);
			}
		}
	}

	private void clear() {
		api.removeAllWaypoints(RailwayGps.MOD_ID);
		for (PolygonOverlay overlay : overlays) {
			api.remove(overlay);
		}
		overlays.clear();
	}

	private void addControlPoint(ControlPoint point) {
		ZoneBox first = point.zone().boxes().getFirst();
		TrackPoint center = new TrackPoint(
				(first.min().x() + first.max().x()) / 2,
				(first.min().y() + first.max().y()) / 2,
				(first.min().z() + first.max().z()) / 2);
		int color = point.kind() == ControlPoint.Kind.STATION ? STATION_COLOR : JUNCTION_COLOR;
		addWaypoint("control-" + point.id(), point.name(), "Railway GPS " + point.kind().name().toLowerCase(),
				point.zone().dimension(), center, color);
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

	private void addPhysicalTrack(PhysicalTrack track, RailwayNetwork network, boolean highlighted) {
		String labelNames = network.labels().values().stream()
				.filter(label -> label.ranges().stream().anyMatch(range -> range.trackId().equals(track.id())))
				.sorted(java.util.Comparator.comparingInt(TrackLabel::priority).reversed())
				.map(TrackLabel::name).distinct().collect(java.util.stream.Collectors.joining(", "));
		String title = labelNames.isEmpty() ? track.id() : labelNames + " (" + track.id() + ")";
		int color = highlighted ? ACTIVE_ROUTE_COLOR : network.labels().values().stream()
				.filter(label -> label.color() != null
						&& label.ranges().stream().anyMatch(range -> range.trackId().equals(track.id())))
				.sorted(java.util.Comparator.comparingInt(TrackLabel::priority).reversed())
				.map(TrackLabel::color).findFirst().orElse(PHYSICAL_TRACK_COLOR);
		for (int index = 1; index < track.points().size(); index++) {
			MapPolygon polygon = segmentPolygon(track.points().get(index - 1), track.points().get(index));
			if (polygon == null) continue;
			ShapeProperties shape = new ShapeProperties().setStrokeWidth(highlighted ? 2.5f : 1.75f).setStrokeColor(color)
					.setStrokeOpacity(highlighted ? 1.0f : 0.9f).setFillColor(color)
					.setFillOpacity(highlighted ? 0.55f : 0.32f);
			PolygonOverlay overlay = new PolygonOverlay(RailwayGps.MOD_ID, dimension(track.dimension()), shape, polygon);
			overlay.setOverlayGroupName(highlighted ? "Railway GPS active route" : "Railway GPS physical tracks")
					.setTitle(title).setDisplayOrder(highlighted ? 2000 : 1200);
			show(overlay, "physical track", track.id());
		}
	}

	private void addZoneBox(ControlPoint point, ZoneBox box) {
		int y = box.max().y();
		MapPolygon polygon = new MapPolygon(
				new BlockPos(box.min().x(), y, box.min().z()),
				new BlockPos(box.max().x() + 1, y, box.min().z()),
				new BlockPos(box.max().x() + 1, y, box.max().z() + 1),
				new BlockPos(box.min().x(), y, box.max().z() + 1));
		ShapeProperties shape = new ShapeProperties().setStrokeWidth(2f).setStrokeColor(CONTROL_ZONE_COLOR)
				.setStrokeOpacity(0.95f).setFillColor(CONTROL_ZONE_COLOR).setFillOpacity(0.12f);
		PolygonOverlay overlay = new PolygonOverlay(RailwayGps.MOD_ID, dimension(point.zone().dimension()), shape, polygon);
		overlay.setOverlayGroupName("Railway GPS control zones")
				.setTitle(point.name() + " zone (" + point.id() + ")").setDisplayOrder(1100);
		show(overlay, "control zone", point.id());
	}

	private void show(PolygonOverlay overlay, String kind, String id) {
		try {
			api.show(overlay);
			overlays.add(overlay);
		} catch (Exception exception) {
			RailwayGps.LOGGER.warn("Could not show JourneyMap {} overlay for '{}'", kind, id, exception);
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

	private static Set<String> remainingTrackIds(Optional<PhysicalRoutePlan> plan, int currentLeg) {
		Set<String> ids = new HashSet<>();
		plan.ifPresent(value -> {
			for (int index = Math.max(0, currentLeg); index < value.legs().size(); index++) {
				ids.add(value.legs().get(index).track().id());
			}
		});
		return ids;
	}
}
