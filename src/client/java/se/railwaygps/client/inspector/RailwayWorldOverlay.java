package se.railwaygps.client.inspector;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.Vec3;
import se.railwaygps.client.ClientRailwayNetwork;
import se.railwaygps.client.navigation.ClientNavigator;
import se.railwaygps.model.TrackPoint;
import se.railwaygps.railway.model.PhysicalTrack;
import se.railwaygps.railway.model.TrackLabel;
import se.railwaygps.railway.model.ZoneBox;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Optional native world overlay for inspecting the synchronized railway graph. */
public final class RailwayWorldOverlay {
	private static final double MAX_DISTANCE_SQUARED = 256 * 256;
	private static final int ZONE_COLOR = 0xE6F2C94C;
	private static final int TRACK_COLOR = 0xD94DB7E5;
	private static final int ACTIVE_COLOR = 0xFF35B9F3;
	private static boolean enabled;
	private static ZoneBox selectionPreview;
	private static String highlightedFirstTrack;
	private static String highlightedSecondTrack;

	private RailwayWorldOverlay() { }

	public static void register() {
		LevelRenderEvents.AFTER_TRANSLUCENT_FEATURES.register(RailwayWorldOverlay::render);
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
		if (!enabled || client.level == null || context.levelState().cameraRenderState.pos == null) return;
		String dimension = client.level.dimension().identifier().toString();
		Vec3 camera = context.levelState().cameraRenderState.pos;
		List<Line> lines = new ArrayList<>();

		ClientRailwayNetwork.network().controlPoints().values().stream()
				.filter(point -> point.zone().dimension().equals(dimension))
				.forEach(point -> point.zone().boxes().stream()
						.filter(box -> near(camera, box))
						.forEach(box -> addBox(lines, box, ZONE_COLOR)));
		if (selectionPreview != null && near(camera, selectionPreview)) {
			addBox(lines, selectionPreview, 0xFFF2C94C);
		}

		Set<String> activeTracks = new HashSet<>();
		ClientNavigator.activePlan().ifPresent(plan -> plan.legs().forEach(leg -> activeTracks.add(leg.track().id())));
		for (PhysicalTrack track : ClientRailwayNetwork.network().tracks().values()) {
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
