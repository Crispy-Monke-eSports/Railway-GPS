package se.railwaygps.client.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import se.railwaygps.client.ClientRailwayNetwork;
import se.railwaygps.client.inspector.RailwayWorldOverlay;
import se.railwaygps.railway.model.PhysicalTrack;
import se.railwaygps.railway.model.TrackDirection;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Explicitly verifies another direction on one existing physical geometry. */
public final class TrackDirectionScreen extends Screen {
	private final List<Button> rows = new ArrayList<>();
	private final Set<String> locallyHandled = new HashSet<>();
	private List<MissingDirection> candidates = List.of();
	private MissingDirection selected;
	private Button approve;
	private boolean overlayWasEnabled;

	public TrackDirectionScreen() {
		super(Component.translatable("railwaygps.direction.title"));
	}

	@Override
	protected void init() {
		if (!RailwayGpsUi.canAdminister(minecraft)) {
			minecraft.gui.setScreen(new NavigationScreen());
			return;
		}
		overlayWasEnabled = RailwayWorldOverlay.enabled();
		if (!overlayWasEnabled) RailwayWorldOverlay.toggle();
		int panelWidth = Math.min(460, width - 32);
		int left = (width - panelWidth) / 2;
		for (int slot = 0; slot < 7; slot++) {
			int index = slot;
			rows.add(addRenderableWidget(Button.builder(Component.empty(), button -> select(index))
					.bounds(left + 16, 58 + slot * 24, panelWidth - 32, 20).build()));
		}
		int bottom = height - 42;
		approve = addRenderableWidget(Button.builder(Component.translatable("railwaygps.direction.approve"),
				button -> approve()).bounds(left + 16, bottom, 170, 20).build());
		addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> onClose())
				.bounds(left + panelWidth - 96, bottom, 80, 20).build());
		refresh();
	}

	private void refresh() {
		List<MissingDirection> result = new ArrayList<>();
		for (PhysicalTrack track : ClientRailwayNetwork.network().tracks().values()) {
			if (track.status() != PhysicalTrack.Status.VERIFIED) continue;
			for (TrackDirection direction : TrackDirection.values()) {
				MissingDirection candidate = new MissingDirection(track.id(), direction,
						direction == TrackDirection.FORWARD ? track.fromControlPoint() : track.toControlPoint(),
						direction == TrackDirection.FORWARD ? track.toControlPoint() : track.fromControlPoint());
				if (!track.directions().contains(direction) && !locallyHandled.contains(candidate.key())) result.add(candidate);
			}
		}
		candidates = result.stream().limit(rows.size()).toList();
		for (int index = 0; index < rows.size(); index++) {
			Button row = rows.get(index);
			if (index >= candidates.size()) {
				row.visible = false;
				continue;
			}
			MissingDirection candidate = candidates.get(index);
			row.visible = true;
			row.setMessage(Component.translatable("railwaygps.direction.row", shortId(candidate.trackId),
					candidate.from, candidate.to));
		}
		approve.active = selected != null;
	}

	private void select(int index) {
		if (index >= candidates.size()) return;
		selected = candidates.get(index);
		RailwayWorldOverlay.setHighlightedTrackPair(selected.trackId, null);
		refresh();
	}

	private void approve() {
		if (selected == null || minecraft.getConnection() == null) return;
		minecraft.getConnection().sendCommand("rgps track allow-direction " + selected.trackId + " "
				+ selected.direction.name().toLowerCase());
		locallyHandled.add(selected.key());
		selected = null;
		RailwayWorldOverlay.setHighlightedTrackPair(null, null);
		refresh();
	}

	@Override
	public void onClose() {
		RailwayWorldOverlay.setHighlightedTrackPair(null, null);
		if (!overlayWasEnabled && RailwayWorldOverlay.enabled()) RailwayWorldOverlay.toggle();
		minecraft.gui.setScreen(new AdminScreen());
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		extractTransparentBackground(graphics);
		int panelWidth = Math.min(460, width - 32);
		int left = (width - panelWidth) / 2;
		graphics.fill(left, 12, left + panelWidth, height - 12, 0xE0101519);
		graphics.fill(left, 12, left + 4, height - 12, 0xFF35B9F3);
		graphics.centeredText(font, title, width / 2, 22, 0xFFFFFFFF);
		graphics.centeredText(font, Component.translatable("railwaygps.direction.subtitle"),
				width / 2, 36, 0xFF9EABB3);
		if (candidates.isEmpty()) graphics.centeredText(font, Component.translatable("railwaygps.direction.empty"),
				width / 2, 70, 0xFF9EABB3);
	}

	@Override public boolean isPauseScreen() { return false; }

	private static String shortId(String id) { return id.length() <= 18 ? id : id.substring(0, 15) + "…"; }

	private record MissingDirection(String trackId, TrackDirection direction, String from, String to) {
		private String key() { return trackId + "|" + direction.name(); }
	}
}
