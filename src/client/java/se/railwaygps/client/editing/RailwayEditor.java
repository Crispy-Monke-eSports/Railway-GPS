package se.railwaygps.client.editing;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;
import se.railwaygps.RailwayGps;
import se.railwaygps.client.inspector.RailwayWorldOverlay;
import se.railwaygps.model.TrackPoint;
import se.railwaygps.railway.model.ZoneBox;

/** SimpleBlueprints-inspired in-world administration mode. */
public final class RailwayEditor {
	private static final Identifier HUD_ID = Identifier.fromNamespaceAndPath(RailwayGps.MOD_ID, "editor");
	private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
			Identifier.fromNamespaceAndPath(RailwayGps.MOD_ID, "editing"));
	private static KeyMapping toggle;
	private static boolean enabled;
	private static BlockPos first;
	private static BlockPos second;

	private RailwayEditor() { }

	public static void register() {
		toggle = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.railwaygps.editor",
				InputConstants.Type.KEYBOARD, InputConstants.KEY_B, CATEGORY));
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (toggle.consumeClick()) setEnabled(!enabled);
		});
		AttackBlockCallback.EVENT.register((player, level, hand, position, direction) -> {
			if (!enabled || !level.isClientSide()) return InteractionResult.PASS;
			selectFirst(position);
			return InteractionResult.SUCCESS;
		});
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			if (!enabled || !level.isClientSide()) return InteractionResult.PASS;
			selectSecond(hit.getBlockPos());
			return InteractionResult.SUCCESS;
		});
		HudElementRegistry.addLast(HUD_ID, (graphics, deltaTracker) -> renderHud(graphics));
	}

	public static void clear() {
		setEnabled(false);
	}

	private static void setEnabled(boolean value) {
		enabled = value;
		first = null;
		second = null;
		RailwayWorldOverlay.setSelectionPreview(null);
		if (value && !RailwayWorldOverlay.enabled()) RailwayWorldOverlay.toggle();
		Minecraft client = Minecraft.getInstance();
		if (client.player != null) client.player.sendOverlayMessage(Component.translatable(
				value ? "railwaygps.editor.enabled" : "railwaygps.editor.disabled"));
	}

	private static void selectFirst(BlockPos position) {
		first = position.immutable();
		second = null;
		sendPositionCommand("rgps zone first", first);
		updatePreview();
	}

	private static void selectSecond(BlockPos position) {
		Minecraft client = Minecraft.getInstance();
		if (first == null) {
			if (client.player != null) client.player.sendOverlayMessage(
					Component.translatable("railwaygps.editor.first_required"));
			return;
		}
		second = position.immutable();
		sendPositionCommand("rgps zone second", second);
		updatePreview();
	}

	private static void sendPositionCommand(String command, BlockPos position) {
		Minecraft client = Minecraft.getInstance();
		if (client.getConnection() != null) {
			client.getConnection().sendCommand("%s %d %d %d".formatted(command,
					position.getX(), position.getY(), position.getZ()));
		}
	}

	private static void updatePreview() {
		if (first == null) {
			RailwayWorldOverlay.setSelectionPreview(null);
			return;
		}
		BlockPos end = second == null ? first : second;
		RailwayWorldOverlay.setSelectionPreview(new ZoneBox(
				new TrackPoint(Math.min(first.getX(), end.getX()), Math.min(first.getY(), end.getY()),
						Math.min(first.getZ(), end.getZ())),
				new TrackPoint(Math.max(first.getX(), end.getX()), Math.max(first.getY(), end.getY()),
						Math.max(first.getZ(), end.getZ()))));
	}

	private static void renderHud(net.minecraft.client.gui.GuiGraphicsExtractor graphics) {
		if (!enabled) return;
		Minecraft client = Minecraft.getInstance();
		int width = 285;
		int height = second == null ? 54 : 68;
		int x = 12;
		int y = graphics.guiHeight() - height - 12;
		graphics.fill(x, y, x + width, y + height, 0xD0101519);
		graphics.fill(x, y, x + 3, y + height, 0xFFF2C94C);
		graphics.text(client.font, Component.translatable("railwaygps.editor.title"), x + 10, y + 8, 0xFFFFFFFF, true);
		graphics.text(client.font, Component.translatable("railwaygps.editor.actions"), x + 10, y + 24, 0xFFE2E8EC, false);
		graphics.text(client.font, Component.translatable("railwaygps.editor.selection",
				first == null ? "—" : format(first), second == null ? "—" : format(second)),
				x + 10, y + 39, 0xFF9EABB3, false);
		if (second != null) graphics.text(client.font, Component.translatable("railwaygps.editor.create_hint"),
				x + 10, y + 54, 0xFF65D18A, false);
	}

	private static String format(BlockPos pos) {
		return pos.getX() + "," + pos.getY() + "," + pos.getZ();
	}
}
