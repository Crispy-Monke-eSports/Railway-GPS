package se.railwaygps.client.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import se.railwaygps.client.ClientRailwayNetwork;
import se.railwaygps.client.navigation.ClientNavigator;

public final class NavigationCommands {
	private NavigationCommands() {
	}

	public static void register() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
				ClientCommands.literal("rgpsnav")
						.then(ClientCommands.literal("start")
								.then(ClientCommands.argument("destination", StringArgumentType.word())
										.suggests((context, builder) -> SharedSuggestionProvider.suggest(
												java.util.stream.Stream.concat(
														ClientRailwayNetwork.network().stations().keySet().stream(),
														ClientRailwayNetwork.network().junctions().keySet().stream()), builder))
										.executes(context -> {
											String destination = StringArgumentType.getString(context, "destination");
											ClientNavigator.StartResult result = ClientNavigator.start(context.getSource().getPlayer(), destination);
											if (result.success()) {
												context.getSource().sendFeedback(Component.literal(result.message()));
												return Command.SINGLE_SUCCESS;
											}
											context.getSource().sendError(Component.literal(result.message()));
											return 0;
										})))
						.then(ClientCommands.literal("stop").executes(context -> {
							ClientNavigator.stop();
							context.getSource().sendFeedback(Component.literal("Railway GPS navigation stopped"));
							return Command.SINGLE_SUCCESS;
						}))
						.then(ClientCommands.literal("status").executes(context -> {
							ClientNavigator.snapshot().ifPresentOrElse(
									snapshot -> context.getSource().sendFeedback(Component.literal(
											"Destination: %s, next: %s, %.0f blocks remaining (%s)".formatted(
													snapshot.destination(), snapshot.nextNode(), snapshot.distanceRemaining(), snapshot.status()))),
									() -> context.getSource().sendError(Component.literal("Navigation is not active")));
							return ClientNavigator.snapshot().isPresent() ? Command.SINGLE_SUCCESS : 0;
						}))
		));
	}
}
