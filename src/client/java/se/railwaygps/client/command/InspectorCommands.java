package se.railwaygps.client.command;

import com.mojang.brigadier.Command;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.minecraft.network.chat.Component;
import se.railwaygps.client.ClientRailwayNetwork;
import se.railwaygps.client.ClientPermissions;
import se.railwaygps.client.inspector.RailwayWorldOverlay;

import java.util.Comparator;

public final class InspectorCommands {
	private InspectorCommands() { }

	public static void register() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
				ClientCommands.literal("rgpsinspect")
						.requires(source -> ClientPermissions.get().inspect())
						.then(ClientCommands.literal("toggle").executes(context -> {
							boolean enabled = RailwayWorldOverlay.toggle();
							context.getSource().sendFeedback(Component.translatable(
									enabled ? "railwaygps.inspect.enabled" : "railwaygps.inspect.disabled"));
							return Command.SINGLE_SUCCESS;
						}))
						.then(ClientCommands.literal("status").executes(context -> {
							var network = ClientRailwayNetwork.network();
							context.getSource().sendFeedback(Component.translatable("railwaygps.inspect.status",
									network.controlPoints().size(), network.tracks().size(), network.labels().size()));
							return Command.SINGLE_SUCCESS;
						}))
						.then(ClientCommands.literal("list")
								.then(ClientCommands.literal("controls").executes(context -> {
									ClientRailwayNetwork.network().controlPoints().values().stream()
											.sorted(Comparator.comparing(point -> point.id()))
											.forEach(point -> context.getSource().sendFeedback(Component.literal(
													point.id() + " — " + point.name() + " [" + point.kind().name().toLowerCase() + "]")));
									return Command.SINGLE_SUCCESS;
								}))
								.then(ClientCommands.literal("tracks").executes(context -> {
									ClientRailwayNetwork.network().tracks().values().stream()
											.sorted(Comparator.comparing(track -> track.id()))
											.forEach(track -> context.getSource().sendFeedback(Component.literal(
													track.id() + " — " + track.fromControlPoint() + " → "
															+ track.toControlPoint() + " [" + track.status().name().toLowerCase() + "]")));
									return Command.SINGLE_SUCCESS;
								}))
								.then(ClientCommands.literal("labels").executes(context -> {
									ClientRailwayNetwork.network().labels().values().stream()
											.sorted(Comparator.comparing(label -> label.id()))
											.forEach(label -> context.getSource().sendFeedback(Component.literal(
													label.id() + " — " + label.name() + " [" + label.ranges().size() + " ranges]")));
									return Command.SINGLE_SUCCESS;
								})))
		));
	}
}
