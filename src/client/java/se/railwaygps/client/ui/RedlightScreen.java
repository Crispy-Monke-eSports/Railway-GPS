package se.railwaygps.client.ui;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import se.railwaygps.client.ClientRailwayNetwork;
import se.railwaygps.client.ClientTransitionToggleResult;
import se.railwaygps.client.inspector.RailwayWorldOverlay;
import se.railwaygps.network.TransitionTogglePayload;
import se.railwaygps.network.TransitionToggleResultPayload;
import se.railwaygps.railway.model.ControlPoint;
import se.railwaygps.railway.model.PhysicalTopology;
import se.railwaygps.railway.model.TrackTransition;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Per-passage controls for redlights, with the exterior boundary ports visible in-world. */
public final class RedlightScreen extends Screen {
	private final List<Button> rows = new ArrayList<>();
	private List<TrackTransition> transitions = List.of();
	private TrackTransition selected;
	private Button toggle;
	private Component feedback = Component.empty();
	private long resultRevision;
	private String networkVersion = "";
	private boolean overlayWasEnabled;

	public RedlightScreen() { super(Component.translatable("railwaygps.redlight.title")); }

	@Override protected void init() {
		if (!RailwayGpsUi.canAdminister(minecraft)) { minecraft.gui.setScreen(new NavigationScreen()); return; }
		overlayWasEnabled = RailwayWorldOverlay.enabled();
		if (!overlayWasEnabled) RailwayWorldOverlay.toggle();
		int panelWidth = Math.min(540, width - 32);
		int left = (width - panelWidth) / 2;
		for (int slot = 0; slot < 6; slot++) {
			int index = slot;
			rows.add(addRenderableWidget(Button.builder(Component.empty(), button -> select(index))
					.bounds(left + 16, 62 + slot * 24, panelWidth - 32, 20).build()));
		}
		toggle = addRenderableWidget(Button.builder(Component.empty(), button -> toggle())
				.bounds(left + 16, height - 42, 170, 20).build());
		addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> onClose())
				.bounds(left + panelWidth - 96, height - 42, 80, 20).build());
		resultRevision = ClientTransitionToggleResult.revision();
		refresh();
	}

	@Override public void tick() {
		if (!networkVersion.equals(ClientRailwayNetwork.version())) refresh();
		if (resultRevision != ClientTransitionToggleResult.revision()) {
			resultRevision = ClientTransitionToggleResult.revision();
			TransitionToggleResultPayload result = ClientTransitionToggleResult.get();
			if (result != null) feedback = result.success()
					? Component.translatable("railwaygps.redlight.saved")
					: Component.translatable("railwaygps.redlight.error", result.detail());
		}
	}

	private void refresh() {
		networkVersion = ClientRailwayNetwork.version();
		transitions = ClientRailwayNetwork.network().transitions().values().stream()
				.filter(transition -> {
					ControlPoint point = ClientRailwayNetwork.network().controlPoints().get(transition.controlPointId());
					return point != null && point.isRedlight();
				})
				.sorted(Comparator.comparing(TrackTransition::controlPointId).thenComparing(TrackTransition::id))
				.limit(rows.size()).toList();
		if (selected != null) selected = ClientRailwayNetwork.network().transitions().get(selected.id());
		for (int index = 0; index < rows.size(); index++) {
			Button row = rows.get(index);
			if (index >= transitions.size()) { row.visible = false; continue; }
			TrackTransition transition = transitions.get(index);
			row.visible = true;
			ControlPoint point = ClientRailwayNetwork.network().controlPoints().get(transition.controlPointId());
			row.setMessage(Component.translatable("railwaygps.redlight.row", point.name(),
					shortId(PhysicalTopology.endPortId(transition.incoming())),
					shortId(PhysicalTopology.startPortId(transition.outgoing())),
					Component.translatable(transition.enabled() ? "railwaygps.redlight.on" : "railwaygps.redlight.off")));
		}
		toggle.active = selected != null;
		toggle.setMessage(Component.translatable(selected != null && selected.enabled()
				? "railwaygps.redlight.turn_off" : "railwaygps.redlight.turn_on"));
	}

	private void select(int index) {
		if (index >= transitions.size()) return;
		selected = transitions.get(index);
		RailwayWorldOverlay.setHighlightedTrackPair(selected.incoming().trackId(), selected.outgoing().trackId());
		refresh();
	}

	private void toggle() {
		if (selected == null) return;
		feedback = Component.translatable("railwaygps.redlight.saving");
		ClientPlayNetworking.send(new TransitionTogglePayload(selected.id(), !selected.enabled()));
		toggle.active = false;
	}

	@Override public void onClose() {
		RailwayWorldOverlay.setHighlightedTrackPair(null, null);
		if (!overlayWasEnabled && RailwayWorldOverlay.enabled()) RailwayWorldOverlay.toggle();
		minecraft.gui.setScreen(new AdminScreen());
	}

	@Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		extractTransparentBackground(graphics);
		int panelWidth = Math.min(540, width - 32);
		int left = (width - panelWidth) / 2;
		graphics.fill(left, 12, left + panelWidth, height - 12, 0xE0101519);
		graphics.fill(left, 12, left + 4, height - 12, 0xFFFF5454);
		graphics.centeredText(font, title, width / 2, 22, 0xFFFFFFFF);
		graphics.centeredText(font, Component.translatable("railwaygps.redlight.subtitle"), width / 2, 38, 0xFF9EABB3);
		if (transitions.isEmpty()) graphics.centeredText(font, Component.translatable("railwaygps.redlight.empty"),
				width / 2, 72, 0xFF9EABB3);
		if (selected != null && selected.instruction() != null) graphics.centeredText(font,
				Component.literal(selected.instruction()), width / 2, height - 68, 0xFFF2C94C);
		graphics.centeredText(font, feedback, width / 2, height - 56, 0xFF9EABB3);
	}

	@Override public boolean isPauseScreen() { return false; }
	private static String shortId(String id) { return id.length() <= 18 ? id : id.substring(0, 15) + "…"; }
}
