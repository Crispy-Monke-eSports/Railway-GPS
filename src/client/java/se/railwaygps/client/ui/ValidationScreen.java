package se.railwaygps.client.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import se.railwaygps.client.ClientRailwayNetwork;
import se.railwaygps.client.inspector.RailwayWorldOverlay;
import se.railwaygps.railway.model.NetworkValidationReport;
import se.railwaygps.railway.model.RailwayNetworkValidator;

import java.util.ArrayList;
import java.util.List;

/** In-game validation report for the currently accepted immutable revision. */
public final class ValidationScreen extends Screen {
	private List<Entry> entries = List.of();

	public ValidationScreen() {
		super(Component.translatable("railwaygps.validation.title"));
	}

	@Override
	protected void init() {
		if (!RailwayGpsUi.canInspect(minecraft)) {
			minecraft.gui.setScreen(new NavigationScreen());
			return;
		}
		NetworkValidationReport report = RailwayNetworkValidator.inspect(ClientRailwayNetwork.network());
		List<Entry> found = new ArrayList<>();
		report.errors().forEach(message -> found.add(new Entry(message, true)));
		report.warnings().forEach(message -> found.add(new Entry(message, false)));
		entries = List.copyOf(found);
		int panelWidth = Math.min(560, width - 32);
		int left = (width - panelWidth) / 2;
		addRenderableWidget(Button.builder(Component.translatable(
				RailwayWorldOverlay.enabled() ? "railwaygps.validation.hide_overlay" : "railwaygps.validation.show_overlay"),
				button -> {
					RailwayWorldOverlay.toggle();
					button.setMessage(Component.translatable(RailwayWorldOverlay.enabled()
							? "railwaygps.validation.hide_overlay" : "railwaygps.validation.show_overlay"));
				}).bounds(left + 16, height - 42, 160, 20).build());
		addRenderableWidget(Button.builder(Component.translatable("gui.back"),
				button -> minecraft.gui.setScreen(new ToolsScreen()))
				.bounds(left + panelWidth - 96, height - 42, 80, 20).build());
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		extractTransparentBackground(graphics);
		int panelWidth = Math.min(560, width - 32);
		int left = (width - panelWidth) / 2;
		graphics.fill(left, 12, left + panelWidth, height - 12, 0xE0101519);
		graphics.fill(left, 12, left + 4, height - 12, 0xFFFFC857);
		graphics.centeredText(font, title, width / 2, 22, 0xFFFFFFFF);
		graphics.centeredText(font, Component.translatable("railwaygps.validation.revision",
				ClientRailwayNetwork.network().revision()), width / 2, 39, 0xFF9EABB3);
		if (entries.isEmpty()) {
			graphics.centeredText(font, Component.translatable("railwaygps.validation.clean"),
					width / 2, 72, 0xFF65D18A);
			return;
		}
		int maximum = Math.max(1, (height - 104) / 14);
		for (int index = 0; index < Math.min(maximum, entries.size()); index++) {
			Entry entry = entries.get(index);
			String prefix = entry.error ? "✕ " : "⚠ ";
			String text = font.plainSubstrByWidth(prefix + entry.message, panelWidth - 40);
			graphics.text(font, Component.literal(text), left + 18, 62 + index * 14,
					entry.error ? 0xFFFF6B6B : 0xFFFFC857, false);
		}
	}

	@Override public boolean isPauseScreen() { return false; }

	private record Entry(String message, boolean error) { }
}
