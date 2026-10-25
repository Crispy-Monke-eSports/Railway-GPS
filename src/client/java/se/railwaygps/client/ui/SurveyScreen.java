package se.railwaygps.client.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import se.railwaygps.client.ClientSurveyStatus;
import se.railwaygps.network.SurveyStatusPayload;

/** Visual start/finish/cancel workflow with live, server-authoritative diagnostics. */
public final class SurveyScreen extends Screen {
	private Button start;
	private Button finish;
	private Button cancel;

	public SurveyScreen() {
		super(Component.translatable("railwaygps.survey.title"));
	}

	@Override
	protected void init() {
		if (!RailwayGpsUi.canAdminister(minecraft)) {
			minecraft.gui.setScreen(new NavigationScreen());
			return;
		}
		int panelWidth = Math.min(420, width - 32);
		int left = (width - panelWidth) / 2;
		start = addRenderableWidget(Button.builder(Component.translatable("railwaygps.survey.start"),
				button -> command("rgps survey start")).bounds(left + 16, height - 70, 116, 20).build());
		finish = addRenderableWidget(Button.builder(Component.translatable("railwaygps.survey.finish"),
				button -> command("rgps survey finish")).bounds(left + 138, height - 70, 116, 20).build());
		cancel = addRenderableWidget(Button.builder(Component.translatable("railwaygps.survey.cancel"),
				button -> command("rgps survey cancel")).bounds(left + 260, height - 70, 116, 20).build());
		addRenderableWidget(Button.builder(Component.translatable("gui.back"),
				button -> minecraft.gui.setScreen(new AdminScreen()))
				.bounds(left + panelWidth - 96, height - 38, 80, 20).build());
		updateButtons();
	}

	@Override
	public void tick() {
		updateButtons();
	}

	private void updateButtons() {
		if (start == null) return;
		boolean recording = "recording".equals(ClientSurveyStatus.get().state());
		start.active = !recording;
		finish.active = recording;
		cancel.active = recording;
	}

	private void command(String command) {
		if (minecraft.getConnection() != null) minecraft.getConnection().sendCommand(command);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		extractTransparentBackground(graphics);
		int panelWidth = Math.min(420, width - 32);
		int left = (width - panelWidth) / 2;
		graphics.fill(left, 12, left + panelWidth, height - 12, 0xE0101519);
		graphics.fill(left, 12, left + 4, height - 12, statusColor(ClientSurveyStatus.get().state()));
		graphics.centeredText(font, title, width / 2, 24, 0xFFFFFFFF);
		graphics.centeredText(font, Component.translatable("railwaygps.survey.subtitle"), width / 2, 42, 0xFF9EABB3);

		SurveyStatusPayload status = ClientSurveyStatus.get();
		Component message = status.detail().isBlank()
				? Component.translatable(status.messageKey())
				: Component.translatable(status.messageKey(), status.detail());
		graphics.centeredText(font, message, width / 2, 78,
				"error".equals(status.state()) ? 0xFFFF6B6B : 0xFFFFFFFF);
		graphics.centeredText(font, Component.translatable("railwaygps.survey.metrics", status.pointCount(),
				status.visitedControlPoints(), String.format("%.1f", status.distance()),
				String.format("%.1f", status.elapsedTicks() / 20.0)), width / 2, 102, 0xFF9EABB3);
		graphics.centeredText(font, Component.translatable("railwaygps.survey.safety"), width / 2, 128, 0xFF65D18A);
	}

	private static int statusColor(String state) {
		return switch (state) {
			case "recording" -> 0xFF65D18A;
			case "error" -> 0xFFFF6B6B;
			case "finished" -> 0xFF5DA9FF;
			default -> 0xFFF2C94C;
		};
	}

	@Override public boolean isPauseScreen() { return false; }
}
