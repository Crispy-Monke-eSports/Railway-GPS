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
import se.railwaygps.network.CreateRedlightPassagePayload;
import se.railwaygps.network.TransitionToggleResultPayload;
import se.railwaygps.railway.editing.RedlightPassageOptions;
import se.railwaygps.railway.model.ControlPoint;
import se.railwaygps.railway.model.RailwayNetwork;
import se.railwaygps.railway.model.PhysicalTrack;
import se.railwaygps.railway.model.RailwayNetworkValidator;
import se.railwaygps.railway.model.TraversalRef;

import java.util.Comparator;
import java.util.List;

/** Build an incoming-to-outgoing redlight passage by choosing existing, verified directed tracks. */
public final class RedlightPassageScreen extends Screen {
	private List<ControlPoint> redlights = List.of();
	private List<TraversalRef> incomingOptions = List.of();
	private List<TraversalRef> outgoingOptions = List.of();
	private String controlPointId;
	private TraversalRef incoming;
	private TraversalRef outgoing;
	private Button controlPrevious;
	private Button controlNext;
	private Button controlLabel;
	private Button incomingPrevious;
	private Button incomingNext;
	private Button incomingLabel;
	private Button outgoingPrevious;
	private Button outgoingNext;
	private Button outgoingLabel;
	private Button create;
	private EditBox instruction;
	private Component feedback = Component.empty();
	private long seenResult;
	private String networkVersion = "";
	private boolean overlayWasEnabled;
	private boolean pending;

	public RedlightPassageScreen() {
		super(Component.translatable("railwaygps.redlight.add_passage"));
	}

	@Override protected void init() {
		if (!RailwayGpsUi.canAdminister(minecraft)) { minecraft.gui.setScreen(new NavigationScreen()); return; }
		overlayWasEnabled = RailwayWorldOverlay.enabled();
		if (!overlayWasEnabled) RailwayWorldOverlay.toggle();
		int panelWidth = Math.min(520, width - 32);
		int left = (width - panelWidth) / 2;
		int labelWidth = panelWidth - 96;
		controlPrevious = addRenderableWidget(Button.builder(Component.literal("◀"), button -> cycleControl(-1))
				.bounds(left + 16, 60, 28, 20).build());
		controlLabel = addRenderableWidget(Button.builder(Component.empty(), button -> { })
				.bounds(left + 48, 60, labelWidth, 20).build());
		controlNext = addRenderableWidget(Button.builder(Component.literal("▶"), button -> cycleControl(1))
				.bounds(left + panelWidth - 44, 60, 28, 20).build());
		incomingPrevious = addRenderableWidget(Button.builder(Component.literal("◀"), button -> cycleIncoming(-1))
				.bounds(left + 16, 90, 28, 20).build());
		incomingLabel = addRenderableWidget(Button.builder(Component.empty(), button -> { })
				.bounds(left + 48, 90, labelWidth, 20).build());
		incomingNext = addRenderableWidget(Button.builder(Component.literal("▶"), button -> cycleIncoming(1))
				.bounds(left + panelWidth - 44, 90, 28, 20).build());
		outgoingPrevious = addRenderableWidget(Button.builder(Component.literal("◀"), button -> cycleOutgoing(-1))
				.bounds(left + 16, 120, 28, 20).build());
		outgoingLabel = addRenderableWidget(Button.builder(Component.empty(), button -> { })
				.bounds(left + 48, 120, labelWidth, 20).build());
		outgoingNext = addRenderableWidget(Button.builder(Component.literal("▶"), button -> cycleOutgoing(1))
				.bounds(left + panelWidth - 44, 120, 28, 20).build());
		instruction = new EditBox(font, left + 16, 150, panelWidth - 32, 20,
				Component.translatable("railwaygps.redlight.instruction"));
		instruction.setMaxLength(300);
		instruction.setHint(Component.translatable("railwaygps.redlight.instruction_hint"));
		addRenderableWidget(instruction);
		create = addRenderableWidget(Button.builder(Component.translatable("railwaygps.redlight.create_passage"),
				button -> save()).bounds(left + 16, height - 42, Math.min(190, panelWidth - 132), 20).build());
		addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> onClose())
				.bounds(left + panelWidth - 96, height - 42, 80, 20).build());
		seenResult = ClientTransitionToggleResult.revision();
		refresh();
	}

	@Override public void tick() {
		if (!networkVersion.equals(ClientRailwayNetwork.version())) refresh();
		if (seenResult != ClientTransitionToggleResult.revision()) {
			seenResult = ClientTransitionToggleResult.revision();
			TransitionToggleResultPayload result = ClientTransitionToggleResult.get();
			pending = false;
			if (result != null) feedback = result.success()
					? Component.translatable("railwaygps.redlight.passage_created")
					: Component.translatable("railwaygps.redlight.error", result.detail());
			refresh();
		}
	}

	private void refresh() {
		RailwayNetwork network = ClientRailwayNetwork.network();
		networkVersion = ClientRailwayNetwork.version();
		redlights = network.controlPoints().values().stream().filter(ControlPoint::isRedlight)
				.sorted(Comparator.comparing(ControlPoint::name).thenComparing(ControlPoint::id)).toList();
		if (redlights.stream().noneMatch(point -> point.id().equals(controlPointId))) {
			controlPointId = redlights.isEmpty() ? null : redlights.getFirst().id();
			incoming = null;
			outgoing = null;
		}
		incomingOptions = controlPointId == null ? List.of() : RedlightPassageOptions.incoming(network, controlPointId);
		outgoingOptions = controlPointId == null ? List.of() : RedlightPassageOptions.outgoing(network, controlPointId);
		if (!incomingOptions.contains(incoming)) incoming = incomingOptions.isEmpty() ? null : incomingOptions.getFirst();
		if (!outgoingOptions.contains(outgoing)) outgoing = outgoingOptions.isEmpty() ? null : outgoingOptions.getFirst();
		int controlIndex = indexOfControl();
		controlPrevious.active = controlNext.active = redlights.size() > 1 && !pending;
		incomingPrevious.active = incomingNext.active = incomingOptions.size() > 1 && !pending;
		outgoingPrevious.active = outgoingNext.active = outgoingOptions.size() > 1 && !pending;
		controlLabel.active = incomingLabel.active = outgoingLabel.active = false;
		controlLabel.setMessage(Component.translatable("railwaygps.redlight.choose_control",
				controlIndex < 0 ? "—" : redlights.get(controlIndex).name(),
				controlIndex < 0 ? 0 : controlIndex + 1, redlights.size()));
		incomingLabel.setMessage(Component.translatable("railwaygps.redlight.choose_incoming",
				format(incoming, true), incomingOptions.isEmpty() ? 0 : incomingOptions.indexOf(incoming) + 1,
				incomingOptions.size()));
		outgoingLabel.setMessage(Component.translatable("railwaygps.redlight.choose_outgoing",
				format(outgoing, false), outgoingOptions.isEmpty() ? 0 : outgoingOptions.indexOf(outgoing) + 1,
				outgoingOptions.size()));
		boolean duplicate = incoming != null && outgoing != null && network.transitions().values().stream()
				.anyMatch(transition -> transition.controlPointId().equals(controlPointId)
						&& transition.incoming().equals(incoming) && transition.outgoing().equals(outgoing));
		create.active = !pending && incoming != null && outgoing != null && !duplicate;
		instruction.active = !pending;
		RailwayWorldOverlay.setHighlightedTrackPair(incoming == null ? null : incoming.trackId(),
				outgoing == null ? null : outgoing.trackId());
	}

	private int indexOfControl() {
		for (int i = 0; i < redlights.size(); i++) if (redlights.get(i).id().equals(controlPointId)) return i;
		return -1;
	}

	private void cycleControl(int direction) {
		if (pending || redlights.isEmpty()) return;
		controlPointId = redlights.get(Math.floorMod(indexOfControl() + direction, redlights.size())).id();
		incoming = null;
		outgoing = null;
		feedback = Component.empty();
		refresh();
	}

	private void cycleIncoming(int direction) {
		if (pending || incomingOptions.isEmpty()) return;
		incoming = incomingOptions.get(Math.floorMod(incomingOptions.indexOf(incoming) + direction, incomingOptions.size()));
		feedback = Component.empty();
		refresh();
	}

	private void cycleOutgoing(int direction) {
		if (pending || outgoingOptions.isEmpty()) return;
		outgoing = outgoingOptions.get(Math.floorMod(outgoingOptions.indexOf(outgoing) + direction, outgoingOptions.size()));
		feedback = Component.empty();
		refresh();
	}

	private void save() {
		if (!create.active || controlPointId == null || incoming == null || outgoing == null) return;
		pending = true;
		feedback = Component.translatable("railwaygps.redlight.saving");
		ClientPlayNetworking.send(new CreateRedlightPassagePayload(controlPointId,
				incoming.trackId(), incoming.direction().name(),
				outgoing.trackId(), outgoing.direction().name(), instruction.getValue()));
		refresh();
	}

	@Override public void onClose() {
		RailwayWorldOverlay.setHighlightedTrackPair(null, null);
		if (!overlayWasEnabled && RailwayWorldOverlay.enabled()) RailwayWorldOverlay.toggle();
		minecraft.gui.setScreen(new RedlightScreen());
	}

	@Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
		extractTransparentBackground(graphics);
		int panelWidth = Math.min(520, width - 32);
		int left = (width - panelWidth) / 2;
		graphics.fill(left, 12, left + panelWidth, height - 12, 0xE0101519);
		graphics.fill(left, 12, left + 4, height - 12, 0xFFFF5454);
		graphics.centeredText(font, title, width / 2, 22, 0xFFFFFFFF);
		graphics.centeredText(font, Component.translatable("railwaygps.redlight.create_passage_subtitle"),
				width / 2, 38, 0xFF9EABB3);
		if (redlights.isEmpty()) graphics.centeredText(font, Component.translatable("railwaygps.redlight.no_controls"),
				width / 2, 48, 0xFFFFC857);
		else if (incomingOptions.isEmpty() || outgoingOptions.isEmpty())
			graphics.centeredText(font, Component.translatable("railwaygps.redlight.no_tracks"),
					width / 2, 48, 0xFFFFC857);
		graphics.centeredText(font, feedback, width / 2, height - 62, 0xFF9EABB3);
	}

	@Override public boolean isPauseScreen() { return false; }

	private static String format(TraversalRef ref, boolean incoming) {
		if (ref == null) return "—";
		PhysicalTrack track = ClientRailwayNetwork.network().tracks().get(ref.trackId());
		if (track == null) return ref.trackId();
		String otherId = incoming ? RailwayNetworkValidator.startPoint(track, ref.direction())
				: RailwayNetworkValidator.endPoint(track, ref.direction());
		ControlPoint other = ClientRailwayNetwork.network().controlPoints().get(otherId);
		String name = other == null ? otherId : other.name();
		if (name.length() > 16) name = name.substring(0, 13) + "…";
		String id = ref.trackId();
		return name + " [" + id.substring(Math.max(0, id.length() - 6)) + " "
				+ ref.direction().name().charAt(0) + "]";
	}
}
