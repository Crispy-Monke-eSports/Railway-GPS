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
import se.railwaygps.model.NetworkValidator;
import se.railwaygps.model.RailwayNetwork;
import se.railwaygps.model.Route;
import se.railwaygps.model.Station;
import se.railwaygps.model.TrackPoint;
import se.railwaygps.model.TransitionInstructions;
import se.railwaygps.recording.RecordingException;
import se.railwaygps.recording.RecordingManager;
import se.railwaygps.recording.RecordingStatus;
import se.railwaygps.server.RailwayGpsServer;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

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
						.then(Commands.literal("record")
								.then(Commands.literal("start")
										.then(Commands.argument("from", StringArgumentType.word())
												.then(Commands.argument("to", StringArgumentType.word())
														.then(Commands.argument("route_id", StringArgumentType.word())
																	.executes(RgpsCommands::startRecording)))))
								.then(Commands.literal("finish").executes(RgpsCommands::finishRecording))
								.then(Commands.literal("cancel").executes(RgpsCommands::cancelRecording))
								.then(Commands.literal("status").executes(RgpsCommands::recordingStatus)))
						.then(Commands.literal("route")
								.then(Commands.literal("list").executes(RgpsCommands::listRoutes))
								.then(Commands.literal("info")
										.then(Commands.argument("route_id", StringArgumentType.word()).executes(RgpsCommands::routeInfo)))
								.then(Commands.literal("remove")
										.then(Commands.argument("route_id", StringArgumentType.word()).executes(RgpsCommands::removeRoute)))
								.then(Commands.literal("rename")
										.then(Commands.argument("old_id", StringArgumentType.word())
												.then(Commands.argument("new_id", StringArgumentType.word()).executes(RgpsCommands::renameRoute)))))
						.then(Commands.literal("instruction")
								.then(Commands.literal("set")
										.then(Commands.argument("node_id", StringArgumentType.word())
												.then(Commands.literal(TransitionInstructions.START)
														.then(Commands.argument("outgoing_route", StringArgumentType.word())
																.then(Commands.argument("text", StringArgumentType.greedyString())
																		.executes(context -> setInstruction(context, TransitionInstructions.START)))))
												.then(Commands.argument("incoming_route", StringArgumentType.word())
														.then(Commands.argument("outgoing_route", StringArgumentType.word())
																.then(Commands.argument("text", StringArgumentType.greedyString())
																		.executes(RgpsCommands::setInstruction))))))
								.then(Commands.literal("remove")
										.then(Commands.argument("node_id", StringArgumentType.word())
												.then(Commands.literal(TransitionInstructions.START)
														.then(Commands.argument("outgoing_route", StringArgumentType.word())
																.executes(context -> removeInstruction(context, TransitionInstructions.START))))
												.then(Commands.argument("incoming_route", StringArgumentType.word())
														.then(Commands.argument("outgoing_route", StringArgumentType.word())
																.executes(RgpsCommands::removeInstruction)))))
								.then(Commands.literal("list")
										.then(Commands.argument("node_id", StringArgumentType.word())
												.executes(RgpsCommands::listInstructions))))
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

	private static int startRecording(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		ServerPlayer player = source.getPlayerOrException();
		String from = StringArgumentType.getString(context, "from");
		String to = StringArgumentType.getString(context, "to");
		String routeId = StringArgumentType.getString(context, "route_id");
		try {
			RailwayGpsServer.get(source.getServer()).recordings().start(
					player, from, to, routeId, source.getServer().getTickCount());
			source.sendSuccess(() -> Component.literal(
					"Recording route '" + routeId + "' from '" + from + "' to '" + to + "'. Stay in this minecart."), true);
			return Command.SINGLE_SUCCESS;
		} catch (RecordingException exception) {
			return fail(source, exception.getMessage());
		}
	}

	private static int finishRecording(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		ServerPlayer player = source.getPlayerOrException();
		try {
			RecordingManager.FinishedRecording finished = RailwayGpsServer.get(source.getServer()).recordings().finish(player);
			source.sendSuccess(() -> Component.literal(
					"Saved route '%s': %.1f blocks, %d recorded points, %d simplified points".formatted(
							finished.route().id(), finished.route().distance(), finished.rawPointCount(), finished.route().points().size())), true);
			for (String warning : finished.warnings()) {
				source.sendFailure(Component.literal("Warning: " + warning));
			}
			return Command.SINGLE_SUCCESS;
		} catch (RecordingException exception) {
			return fail(source, exception.getMessage());
		}
	}

	private static int cancelRecording(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		try {
			RailwayGpsServer.get(source.getServer()).recordings().cancel(source.getPlayerOrException());
			source.sendSuccess(() -> Component.literal("Recording cancelled; no route was saved"), true);
			return Command.SINGLE_SUCCESS;
		} catch (RecordingException exception) {
			return fail(source, exception.getMessage());
		}
	}

	private static int recordingStatus(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		try {
			RecordingStatus status = RailwayGpsServer.get(source.getServer()).recordings().status(
					source.getPlayerOrException(), source.getServer().getTickCount());
			source.sendSuccess(() -> Component.literal(
					"Recording '%s' (%s -> %s): %d points, %.1f blocks, %.1f seconds".formatted(
							status.routeId(), status.from(), status.to(), status.pointCount(), status.distance(), status.elapsedTicks() / 20.0)), false);
			return Command.SINGLE_SUCCESS;
		} catch (RecordingException exception) {
			return fail(source, exception.getMessage());
		}
	}

	private static int listNetwork(CommandContext<CommandSourceStack> context) {
		CommandSourceStack source = context.getSource();
		RailwayNetwork network = RailwayGpsServer.get(source.getServer()).network();
		source.sendSuccess(() -> Component.literal("Railway GPS: " + summary(network)), false);
		return Command.SINGLE_SUCCESS;
	}

	private static int listRoutes(CommandContext<CommandSourceStack> context) {
		CommandSourceStack source = context.getSource();
		RailwayNetwork network = RailwayGpsServer.get(source.getServer()).network();
		if (network.routes().isEmpty()) {
			source.sendSuccess(() -> Component.literal("No Railway GPS routes are registered"), false);
			return Command.SINGLE_SUCCESS;
		}
		String routes = network.routes().keySet().stream().sorted().collect(java.util.stream.Collectors.joining(", "));
		source.sendSuccess(() -> Component.literal("Routes (" + network.routes().size() + "): " + routes), false);
		return Command.SINGLE_SUCCESS;
	}

	private static int routeInfo(CommandContext<CommandSourceStack> context) {
		CommandSourceStack source = context.getSource();
		String id = StringArgumentType.getString(context, "route_id");
		Route route = RailwayGpsServer.get(source.getServer()).network().routes().get(id);
		if (route == null) {
			return fail(source, "Unknown route '" + id + "'");
		}
		source.sendSuccess(() -> Component.literal(
				"%s (%s): %s -> %s, %.1f blocks, %d points, %s".formatted(
						route.name(), route.id(), route.from(), route.to(), route.distance(), route.points().size(),
						route.bidirectional() ? "bidirectional" : "directed")), false);
		return Command.SINGLE_SUCCESS;
	}

	private static int removeRoute(CommandContext<CommandSourceStack> context) {
		CommandSourceStack source = context.getSource();
		String id = StringArgumentType.getString(context, "route_id");
		RailwayGpsServer state = RailwayGpsServer.get(source.getServer());
		if (!state.network().routes().containsKey(id)) {
			return fail(source, "Unknown route '" + id + "'");
		}
		if (instructionReferencesRoute(state.network(), id)) {
			return fail(source, "Route '" + id + "' is used by a node instruction; remove that instruction first");
		}
		try {
			state.storage().removeRoute(id);
			state.reload();
			source.sendSuccess(() -> Component.literal("Removed route '" + id + "'"), true);
			return Command.SINGLE_SUCCESS;
		} catch (IOException | NetworkValidationException | IllegalArgumentException exception) {
			return fail(source, "Could not remove route: " + exception.getMessage());
		}
	}

	private static int renameRoute(CommandContext<CommandSourceStack> context) {
		CommandSourceStack source = context.getSource();
		String oldId = StringArgumentType.getString(context, "old_id");
		String newId = StringArgumentType.getString(context, "new_id");
		RailwayGpsServer state = RailwayGpsServer.get(source.getServer());
		Route route = state.network().routes().get(oldId);
		if (route == null) {
			return fail(source, "Unknown route '" + oldId + "'");
		}
		if (state.network().routes().containsKey(newId)) {
			return fail(source, "Route '" + newId + "' already exists");
		}
		if (instructionReferencesRoute(state.network(), oldId)) {
			return fail(source, "Route '" + oldId + "' is used by a node instruction; remove that instruction before renaming it");
		}
		try {
			state.storage().renameRoute(route, newId);
			state.reload();
			source.sendSuccess(() -> Component.literal("Renamed route '" + oldId + "' to '" + newId + "'"), true);
			return Command.SINGLE_SUCCESS;
		} catch (IOException | NetworkValidationException | IllegalArgumentException exception) {
			return fail(source, "Could not rename route: " + exception.getMessage());
		}
	}

	private static int setInstruction(CommandContext<CommandSourceStack> context) {
		return setInstruction(context, StringArgumentType.getString(context, "incoming_route"));
	}

	private static int setInstruction(CommandContext<CommandSourceStack> context, String incomingId) {
		CommandSourceStack source = context.getSource();
		String nodeId = StringArgumentType.getString(context, "node_id");
		String outgoingId = StringArgumentType.getString(context, "outgoing_route");
		String instruction = StringArgumentType.getString(context, "text").trim();
		RailwayGpsServer state = RailwayGpsServer.get(source.getServer());
		RailwayNetwork network = state.network();
		String error = validateInstruction(network, nodeId, incomingId, outgoingId, instruction);
		if (error != null) {
			return fail(source, error);
		}

		Map<String, String> instructions = new TreeMap<>(network.nodeInstructions(nodeId));
		instructions.put(TransitionInstructions.key(incomingId, outgoingId), instruction);
		try {
			updateNode(state, nodeId, instructions);
			state.reload();
			source.sendSuccess(() -> Component.literal("Saved instruction at '" + nodeId + "': " + instruction), true);
			return Command.SINGLE_SUCCESS;
		} catch (IOException | NetworkValidationException | IllegalArgumentException exception) {
			return fail(source, "Could not save instruction: " + exception.getMessage());
		}
	}

	private static int removeInstruction(CommandContext<CommandSourceStack> context) {
		return removeInstruction(context, StringArgumentType.getString(context, "incoming_route"));
	}

	private static int removeInstruction(CommandContext<CommandSourceStack> context, String incomingId) {
		CommandSourceStack source = context.getSource();
		String nodeId = StringArgumentType.getString(context, "node_id");
		String outgoingId = StringArgumentType.getString(context, "outgoing_route");
		RailwayGpsServer state = RailwayGpsServer.get(source.getServer());
		if (!state.network().containsNode(nodeId)) {
			return fail(source, "Unknown node '" + nodeId + "'");
		}
		Map<String, String> instructions = new TreeMap<>(state.network().nodeInstructions(nodeId));
		String key = TransitionInstructions.key(incomingId, outgoingId);
		if (instructions.remove(key) == null) {
			return fail(source, "No instruction '" + key + "' exists at node '" + nodeId + "'");
		}
		try {
			updateNode(state, nodeId, instructions);
			state.reload();
			source.sendSuccess(() -> Component.literal("Removed instruction '" + key + "' from '" + nodeId + "'"), true);
			return Command.SINGLE_SUCCESS;
		} catch (IOException | NetworkValidationException | IllegalArgumentException exception) {
			return fail(source, "Could not remove instruction: " + exception.getMessage());
		}
	}

	private static int listInstructions(CommandContext<CommandSourceStack> context) {
		CommandSourceStack source = context.getSource();
		String nodeId = StringArgumentType.getString(context, "node_id");
		RailwayNetwork network = RailwayGpsServer.get(source.getServer()).network();
		if (!network.containsNode(nodeId)) {
			return fail(source, "Unknown node '" + nodeId + "'");
		}
		Map<String, String> instructions = network.nodeInstructions(nodeId);
		if (instructions.isEmpty()) {
			source.sendSuccess(() -> Component.literal("No custom instructions at '" + nodeId + "'"), false);
			return Command.SINGLE_SUCCESS;
		}
		source.sendSuccess(() -> Component.literal("Custom instructions at '" + nodeId + "':"), false);
		instructions.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry ->
				source.sendSuccess(() -> Component.literal("  " + entry.getKey() + ": " + entry.getValue()), false));
		return Command.SINGLE_SUCCESS;
	}

	private static String validateInstruction(RailwayNetwork network, String nodeId, String incomingId,
			String outgoingId, String text) {
		if (!network.containsNode(nodeId)) {
			return "Unknown node '" + nodeId + "'";
		}
		if (text.isEmpty() || text.length() > 256) {
			return "Instruction text must contain 1 to 256 characters";
		}
		Route outgoing = network.routes().get(outgoingId);
		if (outgoing == null || !NetworkValidator.canDepart(outgoing, nodeId)) {
			return "Route '" + outgoingId + "' cannot depart from node '" + nodeId + "'";
		}
		if (!TransitionInstructions.START.equals(incomingId)) {
			Route incoming = network.routes().get(incomingId);
			if (incoming == null || !NetworkValidator.canArrive(incoming, nodeId)) {
				return "Route '" + incomingId + "' cannot arrive at node '" + nodeId + "'";
			}
		}
		return null;
	}

	private static void updateNode(RailwayGpsServer state, String nodeId, Map<String, String> instructions) throws IOException {
		Station station = state.network().stations().get(nodeId);
		if (station != null) {
			state.storage().updateStation(new Station(station.id(), station.name(), station.dimension(), station.position(), instructions));
			return;
		}
		Junction junction = state.network().junctions().get(nodeId);
		state.storage().updateJunction(new Junction(
				junction.id(), junction.name(), junction.dimension(), junction.position(), junction.radius(), instructions));
	}

	private static boolean instructionReferencesRoute(RailwayNetwork network, String routeId) {
		return java.util.stream.Stream.concat(network.stations().keySet().stream(), network.junctions().keySet().stream())
				.flatMap(node -> network.nodeInstructions(node).keySet().stream())
				.map(TransitionInstructions::parse)
				.flatMap(java.util.Optional::stream)
				.anyMatch(transition -> routeId.equals(transition.incomingRoute()) || routeId.equals(transition.outgoingRoute()));
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
