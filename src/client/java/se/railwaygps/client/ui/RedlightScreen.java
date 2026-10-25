package se.railwaygps.client.ui;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import se.railwaygps.client.ClientRailwayNetwork;
import se.railwaygps.client.ClientTransitionToggleResult;
import se.railwaygps.client.inspector.RailwayWorldOverlay;
import se.railwaygps.network.TransitionTogglePayload;
import se.railwaygps.network.TransitionInstructionPayload;
import se.railwaygps.network.TransitionToggleResultPayload;
import se.railwaygps.railway.model.ControlPoint;
import se.railwaygps.railway.model.PhysicalTopology;
import se.railwaygps.railway.model.TrackTransition;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Per-passage controls for redlights, with the exterior boundary ports visible in-world. */
public final class RedlightScreen extends Screen {
	private static final int PAGE_SIZE = 6;
	private final List<Button> rows = new ArrayList<>();
	private int page;
	private Button previousPage;
	private Button nextPage;
	private List<TrackTransition> transitions = List.of();
	private TrackTransition selected;
	private Button toggle;
	private Button saveInstruction;
	private EditBox instruction;
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
		for (int slot = 0; slot < PAGE_SIZE; slot++) {
			int index = slot;
			rows.add(addRenderableWidget(Button.builder(Component.empty(), button -> select(index))
					.bounds(left + 16, 62 + slot * 24, panelWidth - 32, 20).build()));
		}
		previousPage = addRenderableWidget(Button.builder(Component.literal("‹"), button -> {
			if (page > 0) {
				page--;
				clearSelection();
				refresh();
			}
		}).bounds(left + 16, 210, 30, 20).build());
		nextPage = addRenderableWidget(Button.builder(Component.literal("›"), button -> {
			if ((page + 1) * PAGE_SIZE < transitions.size()) {
				page++;
				clearSelection();
				refresh();
			}
		}).bounds(left + panelWidth - 46, 210, 30, 20).build());
		toggle = addRenderableWidget(Button.builder(Component.empty(), button -> toggle())
				.bounds(left + 16, height - 42, 170, 20).build());
		instruction = new EditBox(font, left + 16, height - 92, panelWidth - 32, 20,
				Component.translatable("railwaygps.redlight.instruction"));
		instruction.setMaxLength(300);
		instruction.setHint(Component.translatable("railwaygps.redlight.instruction_hint"));
		addRenderableWidget(instruction);
		saveInstruction = addRenderableWidget(Button.builder(Component.translatable("railwaygps.redlight.save_instruction"),
				button -> saveInstruction()).bounds(left + 192, height - 42, 170, 20).build());
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
				.toList();
		page = Math.min(page, Math.max(0, (transitions.size() - 1) / PAGE_SIZE));
		if (selected != null) {
			String selectedId = selected.id();
			selected = transitions.stream().filter(candidate -> candidate.id().equals(selectedId))
					.findFirst().orElse(null);
			if (selected == null) {
				instruction.setValue("");
				RailwayWorldOverlay.setHighlightedTrackPair(null, null);
			}
		}
		for (int index = 0; index < rows.size(); index++) {
			Button row = rows.get(index);
			int absoluteIndex = page * PAGE_SIZE + index;
			if (absoluteIndex >= transitions.size()) { row.visible = false; continue; }
			TrackTransition transition = transitions.get(absoluteIndex);
			row.visible = true;
			ControlPoint point = ClientRailwayNetwork.network().controlPoints().get(transition.controlPointId());
			row.setMessage(Component.translatable("railwaygps.redlight.row", point.name(),
					shortId(PhysicalTopology.endPortId(transition.incoming())),
					shortId(PhysicalTopology.startPortId(transition.outgoing())),
					Component.translatable(transition.enabled() ? "railwaygps.redlight.on" : "railwaygps.redlight.off")));
		}
		previousPage.active = page > 0;
		nextPage.active = (page + 1) * PAGE_SIZE < transitions.size();
		toggle.active = selected != null;
		saveInstruction.active = selected != null;
		instruction.active = selected != null;
		toggle.setMessage(Component.translatable(selected != null && selected.enabled()
				? "railwaygps.redlight.turn_off" : "railwaygps.redlight.turn_on"));
	}

	private void select(int index) {
		int absoluteIndex = page * PAGE_SIZE + index;
		if (absoluteIndex >= transitions.size()) return;
		selected = transitions.get(absoluteIndex);
		instruction.setValue(selected.instruction() == null ? "" : selected.instruction());
		RailwayWorldOverlay.setHighlightedTrackPair(selected.incoming().trackId(), selected.outgoing().trackId());
		refresh();
	}

	private void clearSelection() {
		selected = null;
		instruction.setValue("");
		RailwayWorldOverlay.setHighlightedTrackPair(null, null);
	}

	private void saveInstruction() {
		if (selected == null) return;
		feedback = Component.translatable("railwaygps.redlight.saving");
		ClientPlayNetworking.send(new TransitionInstructionPayload(selected.id(), instruction.getValue()));
		saveInstruction.active = false;
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
		else graphics.centeredText(font, Component.translatable("railwaygps.redlight.page", page + 1,
				(transitions.size() + PAGE_SIZE - 1) / PAGE_SIZE), width / 2, 215, 0xFF9EABB3);
		graphics.centeredText(font, feedback, width / 2, height - 106, 0xFF9EABB3);
	}

	@Override public boolean isPauseScreen() { return false; }
	private static String shortId(String id) { return id.length() <= 18 ? id : id.substring(0, 15) + "…"; }
}
