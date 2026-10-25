package se.railwaygps.client.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.UUID;

/** Command-backed naming workflow; the server remains authoritative for selection and publication. */
public final class NamingScreen extends Screen {
	private static final String[] TYPES = {"way", "bridge", "tunnel", "district", "custom"};
	private EditBox name;
	private EditBox candidate;
	private Button type;
	private Button paired;
	private Button save;
	private int typeIndex;
	private boolean includePaired;

	public NamingScreen() {
		super(Component.translatable("railwaygps.naming.title"));
	}

	@Override
	protected void init() {
		if (!RailwayGpsUi.canName(minecraft)) {
			minecraft.gui.setScreen(new NavigationScreen());
			return;
		}
		int panelWidth = Math.min(440, width - 32);
		int left = (width - panelWidth) / 2;
		name = addRenderableWidget(new EditBox(font, left + 16, 61, panelWidth - 32, 20,
				Component.translatable("railwaygps.naming.name")));
		name.setHint(Component.translatable("railwaygps.naming.name_hint"));
		name.setResponder(value -> updateSave());
		addRenderableWidget(Button.builder(Component.translatable("railwaygps.naming.start"),
				button -> command("rgps label point start")).bounds(left + 16, 91, 128, 20).build());
		addRenderableWidget(Button.builder(Component.translatable("railwaygps.naming.end"),
				button -> command("rgps label point end")).bounds(left + 150, 91, 128, 20).build());
		addRenderableWidget(Button.builder(Component.translatable("railwaygps.naming.cancel"),
				button -> command("rgps label cancel")).bounds(left + 284, 91, 140, 20).build());
		candidate = addRenderableWidget(new EditBox(font, left + 16, 121, 70, 20,
				Component.translatable("railwaygps.naming.candidate")));
		candidate.setValue("1");
		candidate.setResponder(value -> updateSave());
		type = addRenderableWidget(Button.builder(typeMessage(), button -> {
			typeIndex = (typeIndex + 1) % TYPES.length;
			type.setMessage(typeMessage());
		}).bounds(left + 92, 121, 138, 20).build());
		paired = addRenderableWidget(Button.builder(pairedMessage(), button -> {
			includePaired = !includePaired;
			paired.setMessage(pairedMessage());
		}).bounds(left + 236, 121, 188, 20).build());
		save = addRenderableWidget(Button.builder(Component.translatable("railwaygps.naming.save"), button -> save())
				.bounds(left + 16, 151, panelWidth - 32, 22).build());
		addRenderableWidget(Button.builder(Component.translatable("gui.back"),
				button -> minecraft.gui.setScreen(new ToolsScreen())).bounds(left + panelWidth - 96, height - 42, 80, 20).build());
		updateSave();
		setInitialFocus(name);
	}

	private void save() {
		int selected;
		try { selected = Integer.parseInt(candidate.getValue()); }
		catch (NumberFormatException exception) { return; }
		String id = "label_" + UUID.randomUUID().toString().replace("-", "");
		command("rgps label create " + id + " " + TYPES[typeIndex] + " " + selected + " "
				+ includePaired + " \"" + escape(name.getValue().strip()) + "\"");
	}

	private void updateSave() {
		if (save == null) return;
		try { save.active = !name.getValue().isBlank() && Integer.parseInt(candidate.getValue()) > 0; }
		catch (NumberFormatException exception) { save.active = false; }
	}

	private void command(String command) {
		if (minecraft.getConnection() != null) minecraft.getConnection().sendCommand(command);
	}

	private Component typeMessage() {
		return Component.translatable("railwaygps.naming.type", TYPES[typeIndex].toLowerCase(Locale.ROOT));
	}

	private Component pairedMessage() {
		return Component.translatable(includePaired ? "railwaygps.naming.paired_yes" : "railwaygps.naming.paired_no");
	}

	private static String escape(String value) {
		return value.replace("\\", "\\\\").replace("\"", "\\\"");
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		extractTransparentBackground(graphics);
		int panelWidth = Math.min(440, width - 32);
		int left = (width - panelWidth) / 2;
		graphics.fill(left, 12, left + panelWidth, height - 12, 0xE0101519);
		graphics.fill(left, 12, left + 4, height - 12, 0xFF65D18A);
		graphics.centeredText(font, title, width / 2, 24, 0xFFFFFFFF);
		graphics.centeredText(font, Component.translatable("railwaygps.naming.subtitle"), width / 2, 41, 0xFF9EABB3);
		graphics.text(font, Component.translatable("railwaygps.naming.candidate"), left + 16, 113, 0xFF9EABB3, false);
	}

	@Override public boolean isPauseScreen() { return false; }
}
