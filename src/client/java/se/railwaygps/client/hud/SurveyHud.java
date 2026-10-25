package se.railwaygps.client.hud;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import se.railwaygps.RailwayGps;
import se.railwaygps.client.ClientSurveyStatus;
import se.railwaygps.network.SurveyStatusPayload;

/** Minimal recording indicator, visible while driving with survey tools closed. */
public final class SurveyHud {
	private static final Identifier ID = Identifier.fromNamespaceAndPath(RailwayGps.MOD_ID, "survey");

	private SurveyHud() { }

	public static void register() {
		HudElementRegistry.addLast(ID, (graphics, deltaTracker) -> {
			Minecraft client = Minecraft.getInstance();
			if (client.player == null) return;
			SurveyStatusPayload status = ClientSurveyStatus.get();
			if (!"recording".equals(status.state())) return;

			Component message = Component.translatable("railwaygps.survey.hud.recording",
					Math.round(status.distance()));
			int boxWidth = Math.max(126, client.font.width(message) + 20);
			int x = graphics.guiWidth() - boxWidth - 12;
			int y = 12;
			graphics.fill(x, y, x + boxWidth, y + 26, 0xD0101519);
			graphics.fill(x, y, x + 3, y + 26, 0xFF65D18A);
			graphics.text(client.font, message, x + 10, y + 9, 0xFFFFFFFF, true);
		});
	}
}
