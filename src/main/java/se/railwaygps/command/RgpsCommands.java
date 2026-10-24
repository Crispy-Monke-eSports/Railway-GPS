package se.railwaygps.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import se.railwaygps.model.Junction;
import se.railwaygps.model.NetworkValidationException;
import se.railwaygps.model.RailwayNetwork;
import se.railwaygps.model.Station;
import se.railwaygps.model.TrackPoint;
import se.railwaygps.server.RailwayGpsServer;

import java.io.IOException;
import java.util.Locale;

public final class RgpsCommands {
	private static final double DEFAULT_JUNCTION_RADIUS = 25.0;

	private RgpsCommands() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
				Commands.literal("rgps")
						.requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR))
						.then(Commands.literal("station")
								.then(Commands.literal("add")
										.then(Commands.argument("id", StringArgumentType.word())
												.executes(context -> addStation(context, StringArgumentType.getString(context, "id"))))))
						.then(Commands.literal("node")
								.then(Commands.literal("add")
										.then(Commands.argument("id", StringArgumentType.word())
												.executes(context -> addJunction(context, StringArgumentType.getString(context, "id"))))))
						.then(Commands.literal("network")
								.then(Commands.literal("list").executes(RgpsCommands::listNetwork)))
						.then(Commands.literal("reload").executes(RgpsCommands::reload))
		));
	}

	private static int addStation(CommandContext<CommandSourceStack> context, String id) throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		ServerPlayer player = source.getPlayerOrException();
		RailwayGpsServer state = RailwayGpsServer.get(source.getServer());
		if (state.network().containsNode(id)) {
			return fail(source, "A node with id '" + id + "' already exists");
		}

		BlockPos position = player.blockPosition();
		Station station = new Station(
				id,
				toDisplayName(id),
				player.level().dimension().identifier().toString(),
				new TrackPoint(position.getX(), position.getY(), position.getZ())
		);
		try {
			state.storage().createStation(station);
			state.reload();
			source.sendSuccess(() -> Component.literal("Created station '" + id + "' at " + format(position)), true);
			return Command.SINGLE_SUCCESS;
		} catch (IOException | NetworkValidationException | IllegalArgumentException exception) {
			return fail(source, exception.getMessage());
		}
	}

	private static int addJunction(CommandContext<CommandSourceStack> context, String id) throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		ServerPlayer player = source.getPlayerOrException();
		RailwayGpsServer state = RailwayGpsServer.get(source.getServer());
		if (state.network().containsNode(id)) {
			return fail(source, "A node with id '" + id + "' already exists");
		}

		BlockPos position = player.blockPosition();
		Junction junction = new Junction(
				id,
				toDisplayName(id),
				player.level().dimension().identifier().toString(),
				new TrackPoint(position.getX(), position.getY(), position.getZ()),
				DEFAULT_JUNCTION_RADIUS
		);
		try {
			state.storage().createJunction(junction);
			state.reload();
			source.sendSuccess(() -> Component.literal("Created junction '" + id + "' at " + format(position)), true);
			return Command.SINGLE_SUCCESS;
		} catch (IOException | NetworkValidationException | IllegalArgumentException exception) {
			return fail(source, exception.getMessage());
		}
	}

	private static int reload(CommandContext<CommandSourceStack> context) {
		CommandSourceStack source = context.getSource();
		try {
			RailwayNetwork network = RailwayGpsServer.get(source.getServer()).reload();
			source.sendSuccess(() -> Component.literal("Railway GPS reloaded: " + summary(network)), true);
			return Command.SINGLE_SUCCESS;
		} catch (IOException | NetworkValidationException exception) {
			return fail(source, "Reload failed; the previous network remains active. " + exception.getMessage());
		}
	}

	private static int listNetwork(CommandContext<CommandSourceStack> context) {
		CommandSourceStack source = context.getSource();
		RailwayNetwork network = RailwayGpsServer.get(source.getServer()).network();
		source.sendSuccess(() -> Component.literal("Railway GPS: " + summary(network)), false);
		return Command.SINGLE_SUCCESS;
	}

	private static int fail(CommandSourceStack source, String message) {
		source.sendFailure(Component.literal(message == null ? "Unknown Railway GPS error" : message));
		return 0;
	}

	private static String summary(RailwayNetwork network) {
		return "%d stations, %d junctions, %d routes".formatted(
				network.stations().size(), network.junctions().size(), network.routes().size());
	}

	private static String format(BlockPos position) {
		return "%d, %d, %d".formatted(position.getX(), position.getY(), position.getZ());
	}

	private static String toDisplayName(String id) {
		String normalized = id.replace('_', ' ').replace('-', ' ').trim();
		if (normalized.isEmpty()) {
			return id;
		}
		return normalized.substring(0, 1).toUpperCase(Locale.ROOT) + normalized.substring(1);
	}
}
