package se.railwaygps.client.inspector;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.Vec3;
import se.railwaygps.client.ClientRailwayNetwork;
import se.railwaygps.client.navigation.ClientNavigator;
import se.railwaygps.model.TrackPoint;
import se.railwaygps.navigation.RouteOverlayPreference;
import se.railwaygps.railway.model.PhysicalTrack;
import se.railwaygps.railway.model.PhysicalTopology;
import se.railwaygps.railway.model.TrackDirection;
import se.railwaygps.railway.model.TrackLabel;
import se.railwaygps.railway.model.TrackPort;
import se.railwaygps.railway.model.ZoneBox;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.io.IOException;
import java.nio.file.Path;

/** Optional native world overlay for inspecting the synchronized railway graph. */
public final class RailwayWorldOverlay {
	private static final double MAX_DISTANCE_SQUARED = 256 * 256;
	private static final int ZONE_COLOR = 0xE6F2C94C;
	private static final int TRACK_COLOR = 0xD94DB7E5;
	private static final int ACTIVE_COLOR = 0xFF35B9F3;
	private static boolean enabled;
	private static boolean routeEnabled;
	private static final Path ROUTE_SETTING = FabricLoader.getInstance().getConfigDir()
			.resolve("railwaygps-route-overlay.txt");
	private static ZoneBox selectionPreview;
	private static String highlightedFirstTrack;
	private static String highlightedSecondTrack;

	private RailwayWorldOverlay() { }

	public static void register() {
		routeEnabled = RouteOverlayPreference.load(ROUTE_SETTING);
		LevelRenderEvents.AFTER_TRANSLUCENT_FEATURES.register(RailwayWorldOverlay::render);
	}

	public static boolean routeEnabled() { return routeEnabled; }

	public static boolean toggleRoute() {
		routeEnabled = !routeEnabled;
		try { RouteOverlayPreference.save(ROUTE_SETTING, routeEnabled); }
		catch (IOException ignored) { }
		return routeEnabled;
	}

	public static boolean toggle() {
		enabled = !enabled;
		return enabled;
	}

	public static boolean enabled() {
		return enabled;
	}

	public static void setSelectionPreview(ZoneBox preview) {
		selectionPreview = preview;
	}

	public static void setHighlightedTrackPair(String firstTrackId, String secondTrackId) {
		highlightedFirstTrack = firstTrackId;
		highlightedSecondTrack = secondTrackId;
	}

	private static void render(net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext context) {
		Minecraft client = Minecraft.getInstance();
		if ((!enabled && !routeEnabled) || client.level == null || context.levelState().cameraRenderState.pos == null) return;
		String dimension = client.level.dimension().identifier().toString();
		Vec3 camera = context.levelState().cameraRenderState.pos;
		List<Line> lines = new ArrayList<>();

		if (enabled) ClientRailwayNetwork.network().controlPoints().values().stream()
				.filter(point -> point.zone().dimension().equals(dimension))
				.forEach(point -> point.zone().boxes().stream()
						.filter(box -> near(camera, box))
						.forEach(box -> addBox(lines, box, ZONE_COLOR)));
		if (enabled && selectionPreview != null && near(camera, selectionPreview)) {
			addBox(lines, selectionPreview, 0xFFF2C94C);
		}

		Set<String> activeTracks = new HashSet<>();
		ClientNavigator.activePlan().ifPresent(plan -> {
			for (int index = ClientNavigator.currentLegIndex(); index < plan.legs().size(); index++) {
				activeTracks.add(plan.legs().get(index).track().id());
			}
		});
		for (PhysicalTrack track : ClientRailwayNetwork.network().tracks().values()) {
			if (!enabled && !activeTracks.contains(track.id())) continue;
			if (!track.dimension().equals(dimension) || track.points().stream().noneMatch(point -> near(camera, point))) continue;
			boolean highlighted = track.id().equals(highlightedFirstTrack) || track.id().equals(highlightedSecondTrack);
			int color = track.id().equals(highlightedFirstTrack) ? 0xFFFF5DA2
					: track.id().equals(highlightedSecondTrack) ? 0xFFFFB347
					: activeTracks.contains(track.id()) ? ACTIVE_COLOR : labelColor(track.id());
			for (int index = 1; index < track.points().size(); index++) {
				TrackPoint first = track.points().get(index - 1);
				TrackPoint second = track.points().get(index);
				lines.add(new Line(first.x() + 0.5, first.y() + 0.2, first.z() + 0.5,
						second.x() + 0.5, second.y() + 0.2, second.z() + 0.5, color,
						highlighted ? 4f : activeTracks.contains(track.id()) ? 3f : 2f));
			}
			if (enabled) for (TrackDirection direction : track.directions()) addDirectionArrow(lines, track, direction, color);
		}
		if (enabled) for (TrackPort port : PhysicalTopology.from(ClientRailwayNetwork.network()).ports()) {
			PhysicalTrack track = ClientRailwayNetwork.network().tracks().get(port.trackId());
			if (track == null || !track.dimension().equals(dimension) || !near(camera, port.position())) continue;
			double x = port.position().x() + 0.5;
			double y = port.position().y() + 0.35;
			double z = port.position().z() + 0.5;
			lines.add(new Line(x - 0.22, y, z, x + 0.22, y, z, 0xFFF2C94C, 3f));
			lines.add(new Line(x, y, z - 0.22, x, y, z + 0.22, 0xFFF2C94C, 3f));
		}
		if (lines.isEmpty()) return;

		PoseStack poses = context.poseStack();
		poses.pushPose();
		poses.translate(-camera.x(), -camera.y(), -camera.z());
		context.submitNodeCollector().submitCustomGeometry(poses, RenderTypes.linesTranslucentNoDepthWrite(),
				(pose, consumer) -> {
					for (Line line : lines) {
						consumer.addVertex(pose, (float) line.x1, (float) line.y1, (float) line.z1)
								.setColor(line.color).setNormal(pose, 0, 1, 0).setLineWidth(line.width);
						consumer.addVertex(pose, (float) line.x2, (float) line.y2, (float) line.z2)
								.setColor(line.color).setNormal(pose, 0, 1, 0).setLineWidth(line.width);
					}
				});
		poses.popPose();
	}

	private static int labelColor(String trackId) {
		return ClientRailwayNetwork.network().labels().values().stream()
				.filter(label -> label.color() != null
						&& label.ranges().stream().anyMatch(range -> range.trackId().equals(trackId)))
				.sorted(Comparator.comparingInt(TrackLabel::priority).reversed())
				.map(label -> 0xD9000000 | label.color() & 0xFFFFFF)
				.findFirst().orElse(TRACK_COLOR);
	}

	private static void addDirectionArrow(List<Line> lines, PhysicalTrack track, TrackDirection direction, int color) {
		if (track.points().size() < 2) return;
		TrackPoint from = direction == TrackDirection.FORWARD ? track.points().getFirst() : track.points().getLast();
		TrackPoint to = direction == TrackDirection.FORWARD ? track.points().get(1)
				: track.points().get(track.points().size() - 2);
		double dx = to.x() - from.x();
		double dz = to.z() - from.z();
		double length = Math.sqrt(dx * dx + dz * dz);
		if (length < 0.001) return;
		dx /= length;
		dz /= length;
		double tipX = from.x() + 0.5 + dx * 0.8;
		double tipY = from.y() + 0.42;
		double tipZ = from.z() + 0.5 + dz * 0.8;
		double backX = tipX - dx * 0.35;
		double backZ = tipZ - dz * 0.35;
		lines.add(new Line(tipX, tipY, tipZ, backX - dz * 0.22, tipY, backZ + dx * 0.22, color, 3f));
		lines.add(new Line(tipX, tipY, tipZ, backX + dz * 0.22, tipY, backZ - dx * 0.22, color, 3f));
	}

	private static void addBox(List<Line> lines, ZoneBox box, int color) {
		double x1 = box.min().x();
		double y1 = box.min().y();
		double z1 = box.min().z();
		double x2 = box.max().x() + 1;
		double y2 = box.max().y() + 1;
		double z2 = box.max().z() + 1;
		addRectangle(lines, x1, y1, z1, x2, z2, color);
		addRectangle(lines, x1, y2, z1, x2, z2, color);
		lines.add(new Line(x1, y1, z1, x1, y2, z1, color, 2f));
		lines.add(new Line(x2, y1, z1, x2, y2, z1, color, 2f));
		lines.add(new Line(x2, y1, z2, x2, y2, z2, color, 2f));
		lines.add(new Line(x1, y1, z2, x1, y2, z2, color, 2f));
	}

	private static void addRectangle(List<Line> lines, double x1, double y, double z1, double x2, double z2, int color) {
		lines.add(new Line(x1, y, z1, x2, y, z1, color, 2f));
		lines.add(new Line(x2, y, z1, x2, y, z2, color, 2f));
		lines.add(new Line(x2, y, z2, x1, y, z2, color, 2f));
		lines.add(new Line(x1, y, z2, x1, y, z1, color, 2f));
	}

	private static boolean near(Vec3 camera, ZoneBox box) {
		double x = (box.min().x() + box.max().x() + 1) / 2.0;
		double y = (box.min().y() + box.max().y() + 1) / 2.0;
		double z = (box.min().z() + box.max().z() + 1) / 2.0;
		return camera.distanceToSqr(x, y, z) <= MAX_DISTANCE_SQUARED;
	}

	private static boolean near(Vec3 camera, TrackPoint point) {
		return camera.distanceToSqr(point.x() + 0.5, point.y() + 0.5, point.z() + 0.5) <= MAX_DISTANCE_SQUARED;
	}

	private record Line(double x1, double y1, double z1, double x2, double y2, double z2, int color, float width) { }
}
