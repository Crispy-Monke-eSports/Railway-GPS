package se.railwaygps.client.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import se.railwaygps.client.ClientRailwayNetwork;
import se.railwaygps.client.navigation.ClientNavigator;
import se.railwaygps.client.navigation.NavigationStatus;

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
												context.getSource().sendFeedback(result.message());
												return Command.SINGLE_SUCCESS;
											}
											context.getSource().sendError(result.message());
											return 0;
										})))
						.then(ClientCommands.literal("stop").executes(context -> {
							ClientNavigator.stop();
							context.getSource().sendFeedback(Component.translatable("railwaygps.nav.stopped"));
							return Command.SINGLE_SUCCESS;
						}))
						.then(ClientCommands.literal("status").executes(context -> {
							ClientNavigator.snapshot().ifPresentOrElse(
									snapshot -> context.getSource().sendFeedback(Component.translatable(
											"railwaygps.nav.status",
											snapshot.destination(), snapshot.nextNode(),
											Math.round(snapshot.distanceRemaining()), statusText(snapshot.status()))),
									() -> context.getSource().sendError(Component.translatable("railwaygps.nav.not_active")));
							return ClientNavigator.snapshot().isPresent() ? Command.SINGLE_SUCCESS : 0;
						}))
		));
	}

	private static Component statusText(NavigationStatus status) {
		return switch (status) {
			case ON_ROUTE -> Component.translatable("railwaygps.nav.status.on_route");
			case UNCERTAIN -> Component.translatable("railwaygps.nav.status.uncertain");
			case ARRIVED -> Component.translatable("railwaygps.nav.status.arrived");
		};
	}
}
