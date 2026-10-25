package se.railwaygps.client.ui;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import se.railwaygps.RailwayGps;
import se.railwaygps.client.editing.RailwayEditor;

/** Owns the player-facing entry point for navigation and administration. */
public final class RailwayGpsUi {
	private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
			Identifier.fromNamespaceAndPath(RailwayGps.MOD_ID, "controls"));
	private static KeyMapping open;

	private RailwayGpsUi() { }

	public static void register() {
		open = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.railwaygps.open",
				InputConstants.Type.KEYBOARD, InputConstants.KEY_R, CATEGORY));
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (open.consumeClick()) {
				if (client.gui.screen() != null) continue;
				if (RailwayEditor.enabled()) {
					RailwayEditor.finishSelection();
					client.gui.setScreen(new StationCreationScreen());
				}
				else client.gui.setScreen(new NavigationScreen());
			}
		});
	}

	public static boolean canAdminister(Minecraft client) {
		return client.getConnection() != null
				&& client.getConnection().getCommands().getRoot().getChild("rgps") != null;
	}
}
