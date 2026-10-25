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

/** Paginated redlight passage administration, including entry points for creating redlights and passages. */
public final class RedlightScreen extends Screen {
	private final List<Button> rows = new ArrayList<>();
	private List<TrackTransition> transitions = List.of();
	private TrackTransition selected;
	private Button toggle;
	private Button saveInstruction;
	private Button previousPage;
	private Button nextPage;
	private EditBox instruction;
	private Component feedback = Component.empty();
	private long resultRevision;
	private String networkVersion = "";
	private boolean overlayWasEnabled;
	private boolean pending;
	private int page;

	public RedlightScreen() { super(Component.translatable("railwaygps.redlight.title")); }

	@Override protected void init() {
		if (!RailwayGpsUi.canAdminister(minecraft)) { minecraft.gui.setScreen(new NavigationScreen()); return; }
		overlayWasEnabled = RailwayWorldOverlay.enabled();
		if (!overlayWasEnabled) RailwayWorldOverlay.toggle();
		int panelWidth = Math.min(540, width - 32);
		int left = (width - panelWidth) / 2;
		rows.clear();
		int slots = Math.max(1, Math.min(6, (height - 240) / 24 + 1));
		for (int slot = 0; slot < slots; slot++) {
			int index = slot;
			rows.add(addRenderableWidget(Button.builder(Component.empty(), button -> select(index))
					.bounds(left + 16, 62 + slot * 24, panelWidth - 32, 20).build()));
		}
		int pageY = 62 + slots * 24 + 4;
		previousPage = addRenderableWidget(Button.builder(Component.literal("◀"), button -> {
			page--;
			refresh();
		}).bounds(left + 16, pageY, 32, 20).build());
		nextPage = addRenderableWidget(Button.builder(Component.literal("▶"), button -> {
			page++;
			refresh();
		}).bounds(left + panelWidth - 48, pageY, 32, 20).build());

		int half = (panelWidth - 36) / 2;
		addRenderableWidget(Button.builder(Component.translatable("railwaygps.redlight.create"),
				button -> openOther(new StationCreationScreen(true)))
				.bounds(left + 16, pageY + 26, half, 20).build());
		addRenderableWidget(Button.builder(Component.translatable("railwaygps.redlight.add_passage"),
				button -> openOther(new RedlightPassageScreen()))
				.bounds(left + 20 + half, pageY + 26, half, 20).build());

		instruction = new EditBox(font, left + 16, height - 85, panelWidth - 32, 20,
				Component.translatable("railwaygps.redlight.instruction"));
		instruction.setMaxLength(300);
		instruction.setHint(Component.translatable("railwaygps.redlight.instruction_hint"));
		addRenderableWidget(instruction);

		int actionWidth = (panelWidth - 40) / 3;
		toggle = addRenderableWidget(Button.builder(Component.empty(), button -> toggle())
				.bounds(left + 16, height - 42, actionWidth, 20).build());
		saveInstruction = addRenderableWidget(Button.builder(Component.translatable("railwaygps.redlight.save_instruction"),
				button -> saveInstruction()).bounds(left + 20 + actionWidth, height - 42, actionWidth, 20).build());
		addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> onClose())
				.bounds(left + 24 + 2 * actionWidth, height - 42, actionWidth, 20).build());
		resultRevision = ClientTransitionToggleResult.revision();
		refresh();
	}

	@Override public void tick() {
		if (!networkVersion.equals(ClientRailwayNetwork.version())) refresh();
		if (resultRevision != ClientTransitionToggleResult.revision()) {
			resultRevision = ClientTransitionToggleResult.revision();
			TransitionToggleResultPayload result = ClientTransitionToggleResult.get();
			pending = false;
			if (result != null) {
				feedback = result.success() ? Component.translatable("railwaygps.redlight.saved")
						: Component.translatable("railwaygps.redlight.error", result.detail());
				if (result.success() && selected != null) {
					TrackTransition updated = ClientRailwayNetwork.network().transitions().get(selected.id());
					if (updated != null) instruction.setValue(updated.instruction() == null ? "" : updated.instruction());
				}
			}
			refresh();
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
		int pageCount = Math.max(1, (transitions.size() + rows.size() - 1) / rows.size());
		page = Math.max(0, Math.min(page, pageCount - 1));
		if (selected != null) {
			TrackTransition updated = ClientRailwayNetwork.network().transitions().get(selected.id());
			if (updated == null) {
				selected = null;
				instruction.setValue("");
				RailwayWorldOverlay.setHighlightedTrackPair(null, null);
			} else selected = updated;
		}
		for (int index = 0; index < rows.size(); index++) {
			Button row = rows.get(index);
			int offset = page * rows.size() + index;
			if (offset >= transitions.size()) { row.visible = false; continue; }
			TrackTransition transition = transitions.get(offset);
			row.visible = true;
			ControlPoint point = ClientRailwayNetwork.network().controlPoints().get(transition.controlPointId());
			Component label = Component.translatable("railwaygps.redlight.row", point.name(),
					shortId(PhysicalTopology.endPortId(transition.incoming())),
					shortId(PhysicalTopology.startPortId(transition.outgoing())),
					Component.translatable(transition.enabled() ? "railwaygps.redlight.on" : "railwaygps.redlight.off"));
			row.setMessage(Component.literal(selected != null && selected.id().equals(transition.id()) ? "▶ " : "")
					.append(label));
		}
		previousPage.active = page > 0;
		nextPage.active = page < pageCount - 1;
		toggle.active = selected != null && !pending;
		saveInstruction.active = selected != null && !pending;
		instruction.active = selected != null && !pending;
		toggle.setMessage(Component.translatable(selected != null && selected.enabled()
				? "railwaygps.redlight.turn_off" : "railwaygps.redlight.turn_on"));
	}

	private void select(int index) {
		int offset = page * rows.size() + index;
		if (offset >= transitions.size()) return;
		selected = transitions.get(offset);
		instruction.setValue(selected.instruction() == null ? "" : selected.instruction());
		RailwayWorldOverlay.setHighlightedTrackPair(selected.incoming().trackId(), selected.outgoing().trackId());
		refresh();
	}

	private void saveInstruction() {
		if (selected == null || pending) return;
		feedback = Component.translatable("railwaygps.redlight.saving");
		pending = true;
		ClientPlayNetworking.send(new TransitionInstructionPayload(selected.id(), instruction.getValue()));
		refresh();
	}

	private void toggle() {
		if (selected == null || pending) return;
		feedback = Component.translatable("railwaygps.redlight.saving");
		pending = true;
		ClientPlayNetworking.send(new TransitionTogglePayload(selected.id(), !selected.enabled()));
		refresh();
	}

	private void openOther(Screen next) {
		RailwayWorldOverlay.setHighlightedTrackPair(null, null);
		if (!overlayWasEnabled && RailwayWorldOverlay.enabled()) RailwayWorldOverlay.toggle();
		minecraft.gui.setScreen(next);
	}

	@Override public void onClose() { openOther(new AdminScreen()); }

	@Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		extractTransparentBackground(graphics);
		int panelWidth = Math.min(540, width - 32);
		int left = (width - panelWidth) / 2;
		graphics.fill(left, 12, left + panelWidth, height - 12, 0xE0101519);
		graphics.fill(left, 12, left + 4, height - 12, 0xFFFF5454);
		graphics.centeredText(font, title, width / 2, 22, 0xFFFFFFFF);
		graphics.centeredText(font, Component.translatable("railwaygps.redlight.subtitle"), width / 2, 38, 0xFF9EABB3);
		if (transitions.isEmpty()) graphics.centeredText(font, Component.translatable("railwaygps.redlight.empty"),
				width / 2, 51, 0xFF9EABB3);
		graphics.centeredText(font, Component.translatable("railwaygps.redlight.page",
				page + 1, Math.max(1, (transitions.size() + rows.size() - 1) / rows.size())),
				width / 2, 62 + rows.size() * 24 + 10, 0xFF9EABB3);
		graphics.centeredText(font, feedback, width / 2, height - 104, 0xFF9EABB3);
	}

	@Override public boolean isPauseScreen() { return false; }
	private static String shortId(String id) { return id.length() <= 18 ? id : id.substring(0, 15) + "…"; }
}
