package se.railwaygps.client.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import se.railwaygps.client.ClientRailwayNetwork;
import se.railwaygps.client.editing.RailwayEditor;
import se.railwaygps.client.navigation.ClientNavigator;
import se.railwaygps.client.navigation.NavigationSnapshot;
import se.railwaygps.railway.model.ControlPoint;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Searchable, player-facing destination picker. */
public final class NavigationScreen extends Screen {
	private final List<Button> destinationButtons = new ArrayList<>();
	private List<ControlPoint> visibleDestinations = List.of();
	private EditBox search;
	private Button previous;
	private Button next;
	private Button start;
	private int page;
	private int pageSize;
	private String selectedId;
	private Component feedback;

	public NavigationScreen() {
		super(Component.translatable("railwaygps.ui.title"));
	}

	@Override
	protected void init() {
		int panelWidth = Math.min(460, width - 32);
		int left = (width - panelWidth) / 2;
		pageSize = Math.max(4, Math.min(9, (height - 168) / 24));

		search = new EditBox(font, left + 16, 51, panelWidth - 104, 20,
				Component.translatable("railwaygps.ui.search"));
		search.setHint(Component.translatable("railwaygps.ui.search_hint"));
		search.setResponder(value -> {
			page = 0;
			refreshDestinations();
		});
		addRenderableWidget(search);

		previous = addRenderableWidget(Button.builder(Component.literal("‹"), button -> {
			if (page > 0) page--;
			refreshRows();
		}).bounds(left + panelWidth - 80, 51, 28, 20).build());
		next = addRenderableWidget(Button.builder(Component.literal("›"), button -> {
			if ((page + 1) * pageSize < visibleDestinations.size()) page++;
			refreshRows();
		}).bounds(left + panelWidth - 46, 51, 28, 20).build());

		for (int slot = 0; slot < pageSize; slot++) {
			int row = slot;
			Button button = Button.builder(Component.empty(), pressed -> select(row))
					.bounds(left + 16, 79 + slot * 24, panelWidth - 32, 20).build();
			destinationButtons.add(addRenderableWidget(button));
		}

		int bottom = height - 42;
		boolean administrator = RailwayGpsUi.canAdminister(minecraft);
		int buttonCount = administrator ? 4 : 3;
		int gap = 6;
		int buttonWidth = (panelWidth - 32 - gap * (buttonCount - 1)) / buttonCount;
		int buttonX = left + 16;
		start = addRenderableWidget(Button.builder(Component.translatable("railwaygps.ui.start"), button -> start())
				.bounds(buttonX, bottom, buttonWidth, 20).build());
		buttonX += buttonWidth + gap;
		Button stop = addRenderableWidget(Button.builder(Component.translatable("railwaygps.ui.stop"), button -> {
			ClientNavigator.stop();
			feedback = Component.translatable("railwaygps.nav.stopped");
		}).bounds(buttonX, bottom, buttonWidth, 20).build());
		stop.active = ClientNavigator.snapshot().isPresent();
		buttonX += buttonWidth + gap;

		if (administrator) {
			addRenderableWidget(Button.builder(Component.translatable("railwaygps.ui.admin"), button -> {
				minecraft.gui.setScreen(null);
				RailwayEditor.open();
			}).bounds(buttonX, bottom, buttonWidth, 20).build());
			buttonX += buttonWidth + gap;
		}
		addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
				.bounds(buttonX, bottom, buttonWidth, 20).build());

		refreshDestinations();
		setInitialFocus(search);
	}

	private void refreshDestinations() {
		String query = search == null ? "" : search.getValue().strip().toLowerCase(Locale.ROOT);
		visibleDestinations = ClientRailwayNetwork.network().controlPoints().values().stream()
				.filter(point -> query.isEmpty() || searchable(point).contains(query))
				.sorted(Comparator.comparingInt((ControlPoint point) -> kindOrder(point.kind()))
						.thenComparing(ControlPoint::name, String.CASE_INSENSITIVE_ORDER))
				.toList();
		int maximumPage = Math.max(0, (visibleDestinations.size() - 1) / pageSize);
		page = Math.min(page, maximumPage);
		refreshRows();
	}

	private void refreshRows() {
		if (destinationButtons.isEmpty()) return;
		for (int slot = 0; slot < destinationButtons.size(); slot++) {
			int index = page * pageSize + slot;
			Button button = destinationButtons.get(slot);
			if (index >= visibleDestinations.size()) {
				button.visible = false;
				continue;
			}
			ControlPoint point = visibleDestinations.get(index);
			button.visible = true;
			button.setMessage(Component.literal((point.id().equals(selectedId) ? "▶ " : "  ") + point.name())
					.append(Component.literal("  ·  ").withColor(0x7F8C93))
					.append(Component.translatable("railwaygps.ui.kind." + point.kind().name().toLowerCase(Locale.ROOT))
							.withColor(0x9EABB3)));
		}
		previous.active = page > 0;
		next.active = (page + 1) * pageSize < visibleDestinations.size();
		start.active = selectedId != null;
	}

	private void select(int row) {
		int index = page * pageSize + row;
		if (index >= visibleDestinations.size()) return;
		selectedId = visibleDestinations.get(index).id();
		feedback = null;
		refreshRows();
	}

	private void start() {
		if (selectedId == null || minecraft.player == null) return;
		ClientNavigator.StartResult result = ClientNavigator.start(minecraft.player, selectedId);
		if (result.success()) {
			minecraft.player.sendSystemMessage(result.message());
			minecraft.gui.setScreen(null);
		} else {
			feedback = result.message();
		}
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		extractTransparentBackground(graphics);
		int panelWidth = Math.min(460, width - 32);
		int left = (width - panelWidth) / 2;
		graphics.fill(left, 12, left + panelWidth, height - 12, 0xE0101519);
		graphics.fill(left, 12, left + 4, height - 12, 0xFF35B9F3);
		graphics.centeredText(font, title, width / 2, 22, 0xFFFFFFFF);
		graphics.centeredText(font, Component.translatable("railwaygps.ui.subtitle"), width / 2, 34, 0xFF9EABB3);
		if (visibleDestinations.isEmpty()) {
			graphics.centeredText(font, Component.translatable("railwaygps.ui.empty"), width / 2, 92, 0xFFFFC857);
		}
		Component status = feedback != null ? feedback : activeStatus();
		if (status != null) graphics.centeredText(font, status, width / 2, height - 57,
				feedback != null ? 0xFFFFC857 : 0xFF65D18A);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private Component activeStatus() {
		NavigationSnapshot snapshot = ClientNavigator.snapshot().orElse(null);
		return snapshot == null ? null : Component.translatable("railwaygps.ui.active", snapshot.destination(),
				Math.round(snapshot.distanceRemaining()));
	}

	private static String searchable(ControlPoint point) {
		return (point.name() + " " + point.id() + " " + point.kind().name()).toLowerCase(Locale.ROOT);
	}

	private static int kindOrder(ControlPoint.Kind kind) {
		return switch (kind) {
			case STATION -> 0;
			case TERMINAL -> 1;
			case JUNCTION -> 2;
			case DIMENSION_GATE -> 3;
		};
	}
}
