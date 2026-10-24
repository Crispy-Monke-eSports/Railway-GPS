package se.railwaygps.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import se.railwaygps.model.NetworkValidationException;
import se.railwaygps.recording.RecordingException;
import se.railwaygps.server.RailwayGpsServer;
import se.railwaygps.railway.editing.RailwayNetworkEditor;
import se.railwaygps.railway.editing.SurveyManager;
import se.railwaygps.railway.editing.ZoneSelectionManager;
import se.railwaygps.railway.model.ControlPoint;
import se.railwaygps.railway.model.RailwayNetwork;
import se.railwaygps.railway.model.TrackDirection;
import se.railwaygps.railway.model.TrackLabel;
import se.railwaygps.railway.model.TraversalRef;
import se.railwaygps.railway.naming.LabelSelectionManager;
import se.railwaygps.railway.naming.TrackLabelEditor;
import se.railwaygps.railway.naming.TrackPathSelector;

import java.io.IOException;
import java.util.Locale;

public final class RgpsCommands {
	private RgpsCommands() { }

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				dispatcher.register(primaryCommand()));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> primaryCommand() {
		var root = Commands.literal("rgps")
				.requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR));
		root.then(Commands.literal("network").then(Commands.literal("list").executes(RgpsCommands::listNetwork)));
		root.then(Commands.literal("zone")
				.then(Commands.literal("first").executes(RgpsCommands::markZoneFirst))
				.then(Commands.literal("second").executes(RgpsCommands::markZoneSecond))
				.then(Commands.literal("cancel").executes(RgpsCommands::cancelZoneSelection)));
		root.then(Commands.literal("control").then(Commands.literal("create")
				.then(Commands.argument("id", StringArgumentType.word())
						.then(Commands.argument("kind", StringArgumentType.word())
								.then(Commands.argument("name", StringArgumentType.greedyString())
										.executes(RgpsCommands::createControlPoint))))));
		root.then(Commands.literal("survey")
				.then(Commands.literal("start").then(Commands.argument("from", StringArgumentType.word())
						.then(Commands.argument("to", StringArgumentType.word())
								.then(Commands.argument("track_id", StringArgumentType.word())
										.executes(RgpsCommands::startSurvey)))))
				.then(Commands.literal("finish").executes(RgpsCommands::finishSurvey))
				.then(Commands.literal("status").executes(RgpsCommands::surveyStatus))
				.then(Commands.literal("cancel").executes(RgpsCommands::cancelSurvey)));
		root.then(Commands.literal("track").then(Commands.literal("pair")
				.then(Commands.argument("first", StringArgumentType.word())
						.then(Commands.argument("second", StringArgumentType.word())
								.executes(RgpsCommands::pairTracks)))));
		root.then(Commands.literal("transition").then(Commands.literal("add")
				.then(Commands.argument("id", StringArgumentType.word())
						.then(Commands.argument("control", StringArgumentType.word())
								.then(Commands.argument("incoming", StringArgumentType.word())
										.then(Commands.argument("incoming_direction", StringArgumentType.word())
												.then(Commands.argument("outgoing", StringArgumentType.word())
														.then(Commands.argument("outgoing_direction", StringArgumentType.word())
																.executes(context -> addTransition(context, null))
																.then(Commands.argument("instruction", StringArgumentType.greedyString())
																		.executes(context -> addTransition(context,
																				StringArgumentType.getString(context, "instruction"))))))))))));
		root.then(Commands.literal("label")
				.then(Commands.literal("point")
						.then(Commands.literal("start").executes(RgpsCommands::markLabelStart))
						.then(Commands.literal("end").executes(RgpsCommands::markLabelEnd)))
				.then(Commands.literal("candidates").executes(RgpsCommands::listLabelCandidates))
				.then(Commands.literal("cancel").executes(RgpsCommands::cancelLabelSelection))
				.then(Commands.literal("create")
						.then(Commands.argument("id", StringArgumentType.word())
								.then(Commands.argument("type", StringArgumentType.word())
										.then(Commands.argument("candidate", IntegerArgumentType.integer(1))
												.then(Commands.argument("include_paired", BoolArgumentType.bool())
														.then(Commands.argument("name", StringArgumentType.greedyString())
																.executes(RgpsCommands::createLabel))))))));
		root.then(Commands.literal("reload").executes(RgpsCommands::reload));
		return root;
	}

	private static int listNetwork(CommandContext<CommandSourceStack> context) {
		CommandSourceStack source = context.getSource();
		RailwayNetwork network = RailwayGpsServer.get(source.getServer()).network();
		source.sendSuccess(() -> Component.literal("Railway GPS revision %d: %s".formatted(
				network.revision(), summary(network))), false);
		return Command.SINGLE_SUCCESS;
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

	private static int markZoneFirst(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		ZoneSelectionManager.Selection selection = RailwayGpsServer.get(source.getServer()).zoneSelections()
				.markFirst(source.getPlayerOrException());
		source.sendSuccess(() -> Component.literal("First zone corner selected at " + format(selection.first())), false);
		return Command.SINGLE_SUCCESS;
	}

	private static int markZoneSecond(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		try {
			ZoneSelectionManager.Selection selection = RailwayGpsServer.get(source.getServer()).zoneSelections()
					.markSecond(source.getPlayerOrException());
			source.sendSuccess(() -> Component.literal("Second zone corner selected at " + format(selection.second())), false);
			return Command.SINGLE_SUCCESS;
		} catch (IllegalStateException exception) {
			return fail(source, exception.getMessage());
		}
	}

	private static int cancelZoneSelection(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		RailwayGpsServer.get(source.getServer()).zoneSelections().clear(source.getPlayerOrException());
		source.sendSuccess(() -> Component.literal("Zone selection cancelled"), false);
		return Command.SINGLE_SUCCESS;
	}

	private static int createControlPoint(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		String id = StringArgumentType.getString(context, "id");
		String name = StringArgumentType.getString(context, "name").trim();
		ControlPoint.Kind kind;
		try {
			kind = ControlPoint.Kind.valueOf(StringArgumentType.getString(context, "kind").toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException exception) {
			return fail(source, "Unknown control point kind; use station, junction, terminal or dimension_gate");
		}
		try {
			RailwayGpsServer state = RailwayGpsServer.get(source.getServer());
			ServerPlayer player = source.getPlayerOrException();
			ControlPoint point = state.zoneSelections().create(player, id, name, kind);
			state.save(RailwayNetworkEditor.addControlPoint(state.network(), id, name, kind, point.zone()));
			state.zoneSelections().clear(player);
			source.sendSuccess(() -> Component.literal("Created " + kind.name().toLowerCase(Locale.ROOT)
					+ " '" + name + "' (" + id + ")"), true);
			return Command.SINGLE_SUCCESS;
		} catch (IOException | NetworkValidationException | IllegalArgumentException | IllegalStateException exception) {
			return fail(source, "Could not create control point: " + exception.getMessage());
		}
	}

	private static int startSurvey(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		try {
			String from = StringArgumentType.getString(context, "from");
			String to = StringArgumentType.getString(context, "to");
			String trackId = StringArgumentType.getString(context, "track_id");
			RailwayGpsServer.get(source.getServer()).surveys().start(source.getPlayerOrException(), from, to,
					trackId, source.getServer().getTickCount());
			source.sendSuccess(() -> Component.literal("Surveying physical track '" + trackId + "' from '"
					+ from + "' to '" + to + "'. Stay in this minecart."), true);
			return Command.SINGLE_SUCCESS;
		} catch (RecordingException exception) {
			return fail(source, exception.getMessage());
		}
	}

	private static int finishSurvey(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		try {
			SurveyManager.FinishedSurvey finished = RailwayGpsServer.get(source.getServer()).surveys()
					.finish(source.getPlayerOrException());
			source.sendSuccess(() -> Component.literal(
					"Saved physical track '%s': %.1f blocks, %d recorded points, %d simplified points".formatted(
							finished.trackId(), finished.length(), finished.rawPointCount(), finished.simplifiedPointCount())), true);
			return Command.SINGLE_SUCCESS;
		} catch (RecordingException exception) {
			return fail(source, exception.getMessage());
		}
	}

	private static int surveyStatus(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		try {
			SurveyManager.Status status = RailwayGpsServer.get(source.getServer()).surveys()
					.status(source.getPlayerOrException(), source.getServer().getTickCount());
			source.sendSuccess(() -> Component.literal(
					"Survey '%s' (%s -> %s): %d points, %.1f blocks, %.1f seconds".formatted(status.trackId(),
							status.from(), status.to(), status.pointCount(), status.distance(), status.elapsedTicks() / 20.0)), false);
			return Command.SINGLE_SUCCESS;
		} catch (RecordingException exception) {
			return fail(source, exception.getMessage());
		}
	}

	private static int cancelSurvey(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		try {
			RailwayGpsServer.get(source.getServer()).surveys().cancel(source.getPlayerOrException());
			source.sendSuccess(() -> Component.literal("Survey cancelled; no physical track was saved"), true);
			return Command.SINGLE_SUCCESS;
		} catch (RecordingException exception) {
			return fail(source, exception.getMessage());
		}
	}

	private static int pairTracks(CommandContext<CommandSourceStack> context) {
		CommandSourceStack source = context.getSource();
		String first = StringArgumentType.getString(context, "first");
		String second = StringArgumentType.getString(context, "second");
		try {
			RailwayGpsServer state = RailwayGpsServer.get(source.getServer());
			state.save(RailwayNetworkEditor.pairTracks(state.network(), first, second));
			source.sendSuccess(() -> Component.literal("Paired directional tracks '" + first + "' and '" + second + "'"), true);
			return Command.SINGLE_SUCCESS;
		} catch (IOException | NetworkValidationException | IllegalArgumentException exception) {
			return fail(source, "Could not pair tracks: " + exception.getMessage());
		}
	}

	private static int addTransition(CommandContext<CommandSourceStack> context, String instruction) {
		CommandSourceStack source = context.getSource();
		try {
			String id = StringArgumentType.getString(context, "id");
			String control = StringArgumentType.getString(context, "control");
			String incoming = StringArgumentType.getString(context, "incoming");
			String outgoing = StringArgumentType.getString(context, "outgoing");
			TrackDirection incomingDirection = parseDirection(context, "incoming_direction");
			TrackDirection outgoingDirection = parseDirection(context, "outgoing_direction");
			RailwayGpsServer state = RailwayGpsServer.get(source.getServer());
			state.save(RailwayNetworkEditor.addTransition(state.network(), id, control,
					new TraversalRef(incoming, incomingDirection), new TraversalRef(outgoing, outgoingDirection), instruction));
			source.sendSuccess(() -> Component.literal("Created transition '" + id + "' at '" + control + "'"), true);
			return Command.SINGLE_SUCCESS;
		} catch (IOException | NetworkValidationException | IllegalArgumentException exception) {
			return fail(source, "Could not create transition: " + exception.getMessage());
		}
	}

	private static int markLabelStart(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		TrackPathSelector.Point point = RailwayGpsServer.get(source.getServer()).labelSelections()
				.markStart(source.getPlayerOrException());
		source.sendSuccess(() -> Component.literal("Label start selected at " + format(point)), false);
		return Command.SINGLE_SUCCESS;
	}

	private static int markLabelEnd(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		try {
			TrackLabelEditor.Preview preview = RailwayGpsServer.get(source.getServer()).labelSelections()
					.markEnd(source.getPlayerOrException());
			if (!preview.successful()) return fail(source, preview.error());
			source.sendSuccess(() -> Component.literal("Label end selected; " + preview.candidates().size()
					+ " candidate path(s). Use /rgps label candidates to inspect them."), false);
			return Command.SINGLE_SUCCESS;
		} catch (IllegalStateException exception) {
			return fail(source, exception.getMessage());
		}
	}

	private static int listLabelCandidates(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		try {
			LabelSelectionManager.Session session = RailwayGpsServer.get(source.getServer()).labelSelections()
					.session(source.getPlayerOrException());
			if (session.preview() == null || !session.preview().successful()) return fail(source, "Select a valid end point first");
			for (int index = 0; index < session.preview().candidates().size(); index++) {
				int number = index + 1;
				TrackPathSelector.Candidate candidate = session.preview().candidates().get(index);
				String tracks = candidate.ranges().stream().map(range -> range.trackId())
						.collect(java.util.stream.Collectors.joining(" -> "));
				source.sendSuccess(() -> Component.literal(
						"Candidate " + number + " (%.1f blocks): ".formatted(candidate.cost()) + tracks), false);
			}
			return Command.SINGLE_SUCCESS;
		} catch (IllegalStateException exception) {
			return fail(source, exception.getMessage());
		}
	}

	private static int cancelLabelSelection(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		RailwayGpsServer.get(source.getServer()).labelSelections().clear(source.getPlayerOrException());
		source.sendSuccess(() -> Component.literal("Label selection cancelled"), false);
		return Command.SINGLE_SUCCESS;
	}

	private static int createLabel(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();
		String id = StringArgumentType.getString(context, "id");
		String name = StringArgumentType.getString(context, "name");
		int candidate = IntegerArgumentType.getInteger(context, "candidate") - 1;
		boolean includePaired = BoolArgumentType.getBool(context, "include_paired");
		TrackLabel.Type type;
		try {
			type = TrackLabel.Type.valueOf(StringArgumentType.getString(context, "type").toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException exception) {
			return fail(source, "Unknown label type; use way, bridge, tunnel, district or custom");
		}
		try {
			RailwayGpsServer state = RailwayGpsServer.get(source.getServer());
			state.labelSelections().create(source.getPlayerOrException(), id, name, type, candidate, includePaired);
			source.sendSuccess(() -> Component.literal("Created label '" + name.trim() + "' (" + id + ")"), true);
			return Command.SINGLE_SUCCESS;
		} catch (IOException | NetworkValidationException | IllegalArgumentException | IllegalStateException exception) {
			return fail(source, "Could not create label: " + exception.getMessage());
		}
	}

	private static TrackDirection parseDirection(CommandContext<CommandSourceStack> context, String argument) {
		try {
			return TrackDirection.valueOf(StringArgumentType.getString(context, argument).toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException exception) {
			throw new IllegalArgumentException("Unknown direction; use forward or reverse");
		}
	}

	private static String summary(RailwayNetwork network) {
		return "%d control points, %d sections, %d physical tracks, %d transitions, %d labels".formatted(
				network.controlPoints().size(), network.sections().size(), network.tracks().size(),
				network.transitions().size(), network.labels().size());
	}

	private static int fail(CommandSourceStack source, String message) {
		source.sendFailure(Component.literal(message == null ? "Unknown Railway GPS error" : message));
		return 0;
	}

	private static String format(BlockPos position) {
		return "%d, %d, %d".formatted(position.getX(), position.getY(), position.getZ());
	}

	private static String format(TrackPathSelector.Point point) {
		return "%.1f, %.1f, %.1f in %s".formatted(point.x(), point.y(), point.z(), point.dimension());
	}
}
