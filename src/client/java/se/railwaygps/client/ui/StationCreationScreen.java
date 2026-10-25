package se.railwaygps.client.ui;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import se.railwaygps.client.ClientStationCreationResult;
import se.railwaygps.client.editing.RailwayEditor;
import se.railwaygps.network.CreateStationPayload;
import se.railwaygps.network.CreateRedlightPayload;
import se.railwaygps.network.StationCreationResultPayload;

/** Two-corner, one-name station creation with no platform or rail picker. */
public final class StationCreationScreen extends Screen {
	private static String draftStationName = "";
	private static String draftRedlightName = "";
	private final boolean redlight;
	private EditBox name;
	private Button create;
	private boolean pending;
	private long seenResult;
	private Component feedback;
	private boolean acknowledgeOverlaps;
	private Button overlapAcknowledgement;

	public StationCreationScreen() {
		this(false);
	}

	public StationCreationScreen(boolean redlight) {
		super(Component.translatable(redlight ? "railwaygps.redlight.create_title" : "railwaygps.station.title"));
		this.redlight = redlight;
	}

	private String draftName() { return redlight ? draftRedlightName : draftStationName; }

	private void setDraftName(String value) {
		if (redlight) draftRedlightName = value;
		else draftStationName = value;
	}

	private String key(String stationKey, String redlightKey) {
		return redlight ? redlightKey : stationKey;
	}

	@Override
	protected void init() {
		if (!RailwayGpsUi.canAdminister(minecraft)) {
			minecraft.gui.setScreen(new NavigationScreen());
			return;
		}
		int panelWidth = Math.min(420, width - 32);
		int left = (width - panelWidth) / 2;
		name = new EditBox(font, left + 16, 68, panelWidth - 32, 20,
				Component.translatable(key("railwaygps.station.name", "railwaygps.redlight.create_name")));
		name.setHint(Component.translatable(key("railwaygps.station.name_hint", "railwaygps.redlight.create_name_hint")));
		name.setValue(draftName());
		name.setResponder(value -> updateCreate());
		addRenderableWidget(name);
		addRenderableWidget(Button.builder(Component.translatable(key("railwaygps.station.select", "railwaygps.redlight.create_select")), button -> {
			setDraftName(name.getValue());
			feedback = null;
			minecraft.gui.setScreen(null);
			RailwayEditor.open(redlight);
		}).bounds(left + 16, 100, panelWidth - 32, 22).build());
		overlapAcknowledgement = addRenderableWidget(Button.builder(Component.empty(), button -> {
			acknowledgeOverlaps = !acknowledgeOverlaps;
			updateOverlapButton();
		}).bounds(left + 16, 128, panelWidth - 32, 20).build());
		updateOverlapButton();
		create = addRenderableWidget(Button.builder(Component.translatable(key("railwaygps.station.create", "railwaygps.redlight.create")), button -> {
			pending = true;
			feedback = Component.translatable("railwaygps.station.saving");
			if (redlight) ClientPlayNetworking.send(new CreateRedlightPayload(name.getValue().trim(), acknowledgeOverlaps));
			else ClientPlayNetworking.send(new CreateStationPayload(name.getValue().trim(), acknowledgeOverlaps));
			button.active = false;
		}).bounds(left + 16, height - 42, 140, 20).build());
		addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> onClose())
				.bounds(left + panelWidth - 96, height - 42, 80, 20).build());
		seenResult = ClientStationCreationResult.revision();
		updateCreate();
		setInitialFocus(name);
	}

	@Override
	public void tick() {
		if (seenResult == ClientStationCreationResult.revision()) return;
		seenResult = ClientStationCreationResult.revision();
		StationCreationResultPayload result = ClientStationCreationResult.get();
		if (result == null) return;
		pending = false;
		feedback = Component.translatable(result.messageKey(), result.detail());
		if (result.success()) {
			RailwayEditor.discardSelection();
			setDraftName("");
			name.setValue("");
		}
		updateCreate();
	}

	private void updateCreate() {
		if (create != null) create.active = !pending && name != null && !name.getValue().trim().isEmpty()
				&& RailwayEditor.hasCompleteSelection();
	}

	private void updateOverlapButton() {
		if (overlapAcknowledgement != null) overlapAcknowledgement.setMessage(Component.translatable(
				acknowledgeOverlaps ? "railwaygps.station.overlap_yes" : "railwaygps.station.overlap_no"));
	}

	@Override
	public void onClose() {
		RailwayEditor.discardSelection();
		setDraftName("");
		minecraft.gui.setScreen(redlight ? new RedlightScreen() : new AdminScreen());
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		extractTransparentBackground(graphics);
		int panelWidth = Math.min(420, width - 32);
		int left = (width - panelWidth) / 2;
		graphics.fill(left, 12, left + panelWidth, height - 12, 0xE0101519);
		graphics.fill(left, 12, left + 4, height - 12, 0xFFF2C94C);
		graphics.centeredText(font, title, width / 2, 24, 0xFFFFFFFF);
		graphics.centeredText(font, Component.translatable(key("railwaygps.station.subtitle", "railwaygps.redlight.create_subtitle")), width / 2, 42, 0xFF9EABB3);
		graphics.centeredText(font, Component.translatable("railwaygps.station.selection",
				RailwayEditor.selectionText()), width / 2, 160,
				RailwayEditor.hasCompleteSelection() ? 0xFF65D18A : 0xFFFFC857);
		graphics.centeredText(font, Component.translatable(key("railwaygps.station.all_rails", "railwaygps.redlight.create_help")), width / 2, 178, 0xFF9EABB3);
		if (feedback != null) graphics.centeredText(font, feedback, width / 2, height - 62, 0xFFFFC857);
	}

	@Override public boolean isPauseScreen() { return false; }
}
