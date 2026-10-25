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
import se.railwaygps.network.StationCreationResultPayload;

/** Two-corner, one-name station creation with no platform or rail picker. */
public final class StationCreationScreen extends Screen {
	private static String draftName = "";
	private EditBox name;
	private Button create;
	private long seenResult;
	private Component feedback;

	public StationCreationScreen() {
		super(Component.translatable("railwaygps.station.title"));
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
				Component.translatable("railwaygps.station.name"));
		name.setHint(Component.translatable("railwaygps.station.name_hint"));
		name.setValue(draftName);
		name.setResponder(value -> updateCreate());
		addRenderableWidget(name);
		addRenderableWidget(Button.builder(Component.translatable("railwaygps.station.select"), button -> {
			draftName = name.getValue();
			feedback = null;
			minecraft.gui.setScreen(null);
			RailwayEditor.open();
		}).bounds(left + 16, 100, panelWidth - 32, 22).build());
		create = addRenderableWidget(Button.builder(Component.translatable("railwaygps.station.create"), button -> {
			feedback = Component.translatable("railwaygps.station.saving");
			ClientPlayNetworking.send(new CreateStationPayload(name.getValue().trim()));
			button.active = false;
		}).bounds(left + 16, height - 42, 140, 20).build());
		addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> {
			RailwayEditor.discardSelection();
			draftName = "";
			minecraft.gui.setScreen(new AdminScreen());
		}).bounds(left + panelWidth - 96, height - 42, 80, 20).build());
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
		feedback = Component.translatable(result.messageKey(), result.detail());
		if (result.success()) {
			RailwayEditor.discardSelection();
			draftName = "";
			name.setValue("");
		}
		updateCreate();
	}

	private void updateCreate() {
		if (create != null) create.active = name != null && !name.getValue().trim().isEmpty()
				&& RailwayEditor.hasCompleteSelection();
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		extractTransparentBackground(graphics);
		int panelWidth = Math.min(420, width - 32);
		int left = (width - panelWidth) / 2;
		graphics.fill(left, 12, left + panelWidth, height - 12, 0xE0101519);
		graphics.fill(left, 12, left + 4, height - 12, 0xFFF2C94C);
		graphics.centeredText(font, title, width / 2, 24, 0xFFFFFFFF);
		graphics.centeredText(font, Component.translatable("railwaygps.station.subtitle"), width / 2, 42, 0xFF9EABB3);
		graphics.centeredText(font, Component.translatable("railwaygps.station.selection",
				RailwayEditor.selectionText()), width / 2, 134,
				RailwayEditor.hasCompleteSelection() ? 0xFF65D18A : 0xFFFFC857);
		graphics.centeredText(font, Component.translatable("railwaygps.station.all_rails"), width / 2, 154, 0xFF9EABB3);
		if (feedback != null) graphics.centeredText(font, feedback, width / 2, height - 62, 0xFFFFC857);
	}

	@Override public boolean isPauseScreen() { return false; }
}
