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
			Component notice = ClientNavigator.notice().orElse(null);
			boolean namedRail = !snapshot.railNames().isBlank() && snapshot.status() == NavigationStatus.ON_ROUTE;
			int height = 72 + (namedRail ? 14 : 0) + (notice == null ? 0 : 14);
			int x = (graphics.guiWidth() - width) / 2;
			int y = 12;
			graphics.fill(x, y, x + width, y + height, 0xB0101519);
			graphics.fill(x, y, x + 3, y + height, statusColor(snapshot.status()));
			graphics.text(client.font, Component.translatable("railwaygps.hud.title", snapshot.destination()),
					x + 10, y + 8, 0xFFFFFFFF, true);
			int row = y + 24;
			if (namedRail) {
				graphics.text(client.font, Component.literal(client.font.plainSubstrByWidth(snapshot.railNames(), width - 20)),
						x + 10, row, 0xFFF2C94C, true);
				row += 14;
			}
			graphics.text(client.font, snapshot.instruction(), x + 10, row, statusColor(snapshot.status()), true);
			if (snapshot.status() != NavigationStatus.ON_ROUTE) return;
			Component redlight = snapshot.insideRedlight()
					? Component.translatable("railwaygps.hud.inside_redlight")
					: Double.isFinite(snapshot.distanceToNext())
					? Component.translatable("railwaygps.hud.next_redlight", Math.round(snapshot.distanceToNext()))
					: Component.translatable("railwaygps.hud.no_redlight");
			graphics.text(client.font, redlight, x + 10, row + 16, 0xFFE2E8EC, true);
			graphics.text(client.font, Component.translatable("railwaygps.hud.remaining",
							Math.round(snapshot.distanceRemaining()), snapshot.junctionsRemaining()),
					x + 10, row + 31, 0xFF9EABB3, true);
			if (notice != null) graphics.text(client.font, notice, x + 10, row + 46, 0xFF64B5F6, true);
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
