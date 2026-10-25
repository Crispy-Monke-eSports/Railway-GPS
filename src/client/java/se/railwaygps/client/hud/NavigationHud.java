package se.railwaygps.client.hud;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import se.railwaygps.RailwayGps;
import se.railwaygps.client.navigation.ClientNavigator;
import se.railwaygps.client.navigation.NavigationSnapshot;
import se.railwaygps.client.navigation.NavigationStatus;

public final class NavigationHud {
	private static final Identifier ID = Identifier.fromNamespaceAndPath(RailwayGps.MOD_ID, "navigation");

	private NavigationHud() {
	}

	public static void register() {
		HudElementRegistry.addLast(ID, (graphics, deltaTracker) -> {
			Minecraft client = Minecraft.getInstance();
			if (client.player == null || !(client.player.getVehicle() instanceof AbstractMinecart)) {
				return;
			}
			NavigationSnapshot snapshot = ClientNavigator.snapshot().orElse(null);
			if (snapshot == null) {
				return;
			}

			int width = 230;
			int height = 72;
			int x = (graphics.guiWidth() - width) / 2;
			int y = 12;
			graphics.fill(x, y, x + width, y + height, 0xB0101519);
			graphics.fill(x, y, x + 3, y + height, statusColor(snapshot.status()));
			graphics.text(client.font, Component.translatable("railwaygps.hud.title", snapshot.destination()),
					x + 10, y + 8, 0xFFFFFFFF, true);
			graphics.text(client.font, snapshot.instruction(), x + 10, y + 24, statusColor(snapshot.status()), true);
			if (snapshot.status() == NavigationStatus.UNCERTAIN) return;
			graphics.text(client.font, Component.translatable("railwaygps.hud.next",
							snapshot.nextNode(), Math.round(snapshot.distanceToNext())),
					x + 10, y + 40, 0xFFE2E8EC, true);
			graphics.text(client.font, Component.translatable("railwaygps.hud.remaining",
							Math.round(snapshot.distanceRemaining()), snapshot.junctionsRemaining()),
					x + 10, y + 55, 0xFF9EABB3, true);
		});
	}

	private static int statusColor(NavigationStatus status) {
		return switch (status) {
			case ON_ROUTE -> 0xFF65D18A;
			case UNCERTAIN -> 0xFFFFC857;
			case ARRIVED -> 0xFF64B5F6;
		};
	}
}
