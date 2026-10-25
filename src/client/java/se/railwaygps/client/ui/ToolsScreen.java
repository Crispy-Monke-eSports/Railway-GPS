package se.railwaygps.client.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Shows only the independent tools authorized by the server capability snapshot. */
public final class ToolsScreen extends Screen {
	public ToolsScreen() {
		super(Component.translatable("railwaygps.tools.title"));
	}

	@Override
	protected void init() {
		if (!RailwayGpsUi.hasTools(minecraft)) {
			minecraft.gui.setScreen(new NavigationScreen());
			return;
		}
		int panelWidth = Math.min(360, width - 32);
		int left = (width - panelWidth) / 2;
		int y = 58;
		if (RailwayGpsUi.canInspect(minecraft)) {
			addRenderableWidget(Button.builder(Component.translatable("railwaygps.tools.inspect"),
					button -> minecraft.gui.setScreen(new ValidationScreen())).bounds(left, y, panelWidth, 24).build());
			y += 30;
		}
		if (RailwayGpsUi.canName(minecraft)) {
			addRenderableWidget(Button.builder(Component.translatable("railwaygps.tools.name"),
					button -> minecraft.gui.setScreen(new NamingScreen())).bounds(left, y, panelWidth, 24).build());
			y += 30;
		}
		if (RailwayGpsUi.canSurvey(minecraft)) {
			addRenderableWidget(Button.builder(Component.translatable("railwaygps.tools.survey"),
					button -> minecraft.gui.setScreen(new SurveyScreen())).bounds(left, y, panelWidth, 24).build());
			y += 30;
		}
		if (RailwayGpsUi.canAdminister(minecraft)) {
			addRenderableWidget(Button.builder(Component.translatable("railwaygps.tools.admin"),
					button -> minecraft.gui.setScreen(new AdminScreen())).bounds(left, y, panelWidth, 24).build());
		}
		addRenderableWidget(Button.builder(Component.translatable("gui.back"),
				button -> minecraft.gui.setScreen(new NavigationScreen()))
				.bounds(left, height - 42, panelWidth, 20).build());
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		extractTransparentBackground(graphics);
		int panelWidth = Math.min(400, width - 24);
		int left = (width - panelWidth) / 2;
		graphics.fill(left, 12, left + panelWidth, height - 12, 0xE0101519);
		graphics.fill(left, 12, left + 4, height - 12, 0xFFF2C94C);
		graphics.centeredText(font, title, width / 2, 25, 0xFFFFFFFF);
		graphics.centeredText(font, Component.translatable("railwaygps.tools.subtitle"), width / 2, 41, 0xFF9EABB3);
	}

	@Override public boolean isPauseScreen() { return false; }
}
