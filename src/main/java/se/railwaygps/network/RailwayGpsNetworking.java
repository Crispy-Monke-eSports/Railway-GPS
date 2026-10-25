package se.railwaygps.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import se.railwaygps.permission.RailwayCapabilities;
import se.railwaygps.permission.RailwayPermission;
import se.railwaygps.permission.RailwayPermissions;
import se.railwaygps.railway.editing.StationCreator;
import se.railwaygps.railway.editing.RailwayNetworkEditor;
import se.railwaygps.server.RailwayGpsServer;
import se.railwaygps.storage.RailwayNetworkJsonCodec;
import se.railwaygps.railway.model.RailwayNetwork;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class RailwayGpsNetworking {
	private static final Map<UUID, RailwayCapabilities> LAST_CAPABILITIES = new HashMap<>();
	private RailwayGpsNetworking() {
	}

	public static void register() {
		PayloadTypeRegistry.clientboundPlay().registerLarge(
				RailwayNetworkPayload.TYPE, RailwayNetworkPayload.CODEC, RailwayNetworkPayload.MAX_PAYLOAD_BYTES);
		PayloadTypeRegistry.clientboundPlay().register(SurveyStatusPayload.TYPE, SurveyStatusPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(StationCreationResultPayload.TYPE, StationCreationResultPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(TransitionToggleResultPayload.TYPE, TransitionToggleResultPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(PermissionSnapshotPayload.TYPE, PermissionSnapshotPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(CreateStationPayload.TYPE, CreateStationPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(TransitionTogglePayload.TYPE, TransitionTogglePayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(TransitionInstructionPayload.TYPE, TransitionInstructionPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(CreateStationPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			if (!RailwayPermissions.has(player, RailwayPermission.ADMIN)) {
				sendStationResult(player, false, "railwaygps.station.error", "You are not allowed to create stations");
				return;
			}
			try {
				StationCreator.create(RailwayGpsServer.get(player.level().getServer()), player, payload.name(),
						payload.acknowledgeOverlaps());
				sendStationResult(player, true, "railwaygps.station.created", payload.name().trim());
			} catch (Exception exception) {
				sendStationResult(player, false, "railwaygps.station.error", exception.getMessage());
			}
		});
		ServerPlayNetworking.registerGlobalReceiver(TransitionTogglePayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			if (!RailwayPermissions.has(player, RailwayPermission.ADMIN)) {
				sendTransitionResult(player, false, "You are not allowed to edit redlight transitions");
				return;
			}
			try {
				RailwayGpsServer state = RailwayGpsServer.get(player.level().getServer());
				state.save(RailwayNetworkEditor.setTransitionEnabled(
						state.network(), payload.transitionId(), payload.enabled()));
				sendTransitionResult(player, true, payload.transitionId());
			} catch (Exception exception) {
				sendTransitionResult(player, false, exception.getMessage());
			}
		});
		ServerPlayNetworking.registerGlobalReceiver(TransitionInstructionPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			if (!RailwayPermissions.has(player, RailwayPermission.ADMIN)) {
				sendTransitionResult(player, false, "You are not allowed to edit redlight instructions");
				return;
			}
			try {
				RailwayGpsServer state = RailwayGpsServer.get(player.level().getServer());
				state.save(RailwayNetworkEditor.setTransitionInstruction(
						state.network(), payload.transitionId(), payload.instruction()));
				sendTransitionResult(player, true, payload.transitionId());
			} catch (Exception exception) {
				sendTransitionResult(player, false, exception.getMessage());
			}
		});
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			RailwayGpsServer state = RailwayGpsServer.get(server);
			send(handler.getPlayer(), state.network());
			sendCapabilities(handler.getPlayer(), true);
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
				LAST_CAPABILITIES.remove(handler.getPlayer().getUUID()));
	}

	/** Refreshes UI visibility without trusting it for authorization. Called once per second. */
	public static void refreshCapabilities(MinecraftServer server) {
		if (server.getTickCount() % 20 != 0) return;
		for (ServerPlayer player : server.getPlayerList().getPlayers()) sendCapabilities(player, false);
	}

	private static void sendCapabilities(ServerPlayer player, boolean force) {
		RailwayCapabilities capabilities = RailwayPermissions.capabilities(player);
		if (!force && capabilities.equals(LAST_CAPABILITIES.get(player.getUUID()))) return;
		LAST_CAPABILITIES.put(player.getUUID(), capabilities);
		if (ServerPlayNetworking.canSend(player, PermissionSnapshotPayload.TYPE)) {
			ServerPlayNetworking.send(player, PermissionSnapshotPayload.from(capabilities));
		}
	}

	private static void sendStationResult(ServerPlayer player, boolean success, String key, String detail) {
		if (ServerPlayNetworking.canSend(player, StationCreationResultPayload.TYPE)) {
			ServerPlayNetworking.send(player, new StationCreationResultPayload(success, key,
					detail == null ? "Unknown error" : detail));
		}
	}

	private static void sendTransitionResult(ServerPlayer player, boolean success, String detail) {
		if (ServerPlayNetworking.canSend(player, TransitionToggleResultPayload.TYPE)) {
			ServerPlayNetworking.send(player, new TransitionToggleResultPayload(success,
					detail == null ? "Unknown error" : detail));
		}
	}

	public static void sendSurveyStatus(ServerPlayer player, SurveyStatusPayload payload) {
		if (ServerPlayNetworking.canSend(player, SurveyStatusPayload.TYPE)) {
			ServerPlayNetworking.send(player, payload);
		}
	}

	public static void broadcast(MinecraftServer server, RailwayNetwork network) {
		RailwayNetworkPayload payload = createPayload(network);
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (ServerPlayNetworking.canSend(player, RailwayNetworkPayload.TYPE)) {
				ServerPlayNetworking.send(player, payload);
			}
		}
	}

	private static void send(ServerPlayer player, RailwayNetwork network) {
		if (ServerPlayNetworking.canSend(player, RailwayNetworkPayload.TYPE)) {
			ServerPlayNetworking.send(player, createPayload(network));
		}
	}

	private static RailwayNetworkPayload createPayload(RailwayNetwork network) {
		try {
			String json = RailwayNetworkJsonCodec.encode(network);
			return new RailwayNetworkPayload(NetworkSnapshotDigest.of(json), json);
		} catch (se.railwaygps.model.NetworkValidationException exception) {
			throw new IllegalStateException("Cannot send an invalid Railway GPS network", exception);
		}
	}
}
