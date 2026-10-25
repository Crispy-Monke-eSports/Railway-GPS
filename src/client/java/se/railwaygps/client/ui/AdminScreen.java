package se.railwaygps.client.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import se.railwaygps.railway.editing.TrackPairingAnalyzer;
import se.railwaygps.client.ClientRailwayNetwork;

/** Role-gated entry point for network administration tools. */
public final class AdminScreen extends Screen {
	public AdminScreen() {
		super(Component.translatable("railwaygps.admin.title"));
	}

	@Override
	protected void init() {
		if (!RailwayGpsUi.canAdminister(minecraft)) {
			minecraft.gui.setScreen(new NavigationScreen());
			return;
		}
		int width = Math.min(320, this.width - 40);
		int left = (this.width - width) / 2;
		int y = 58;
		addRenderableWidget(Button.builder(Component.translatable("railwaygps.admin.zones"), button -> {
			minecraft.gui.setScreen(new StationCreationScreen());
		}).bounds(left, y, width, 24).build());
		int proposals = TrackPairingAnalyzer.proposals(ClientRailwayNetwork.network()).size();
		addRenderableWidget(Button.builder(Component.translatable("railwaygps.admin.pairings", proposals),
				button -> minecraft.gui.setScreen(new PairingScreen()))
				.bounds(left, y + 28, width, 24).build());
		addRenderableWidget(Button.builder(Component.translatable("railwaygps.admin.directions"),
				button -> minecraft.gui.setScreen(new TrackDirectionScreen()))
				.bounds(left, y + 56, width, 24).build());
		addRenderableWidget(Button.builder(Component.translatable("railwaygps.admin.survey"),
				button -> minecraft.gui.setScreen(new SurveyScreen()))
				.bounds(left, y + 84, width, 24).build());
		addRenderableWidget(Button.builder(Component.translatable("railwaygps.admin.redlights"),
				button -> minecraft.gui.setScreen(new RedlightScreen()))
				.bounds(left, y + 112, width, 24).build());
		addRenderableWidget(Button.builder(Component.translatable("railwaygps.admin.validation"),
				button -> minecraft.gui.setScreen(new ValidationScreen()))
				.bounds(left, y + 140, width, 24).build());
		addRenderableWidget(Button.builder(Component.translatable("gui.back"),
				button -> minecraft.gui.setScreen(new NavigationScreen()))
				.bounds(left, this.height - 42, width, 20).build());
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		extractTransparentBackground(graphics);
		int panelWidth = Math.min(360, width - 24);
		int left = (width - panelWidth) / 2;
		graphics.fill(left, 12, left + panelWidth, height - 12, 0xE0101519);
		graphics.fill(left, 12, left + 4, height - 12, 0xFFF2C94C);
		graphics.centeredText(font, title, width / 2, 26, 0xFFFFFFFF);
		graphics.centeredText(font, Component.translatable("railwaygps.admin.subtitle"), width / 2, 42, 0xFF9EABB3);
	}

	@Override public boolean isPauseScreen() { return false; }
}
