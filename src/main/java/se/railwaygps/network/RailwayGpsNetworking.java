package se.railwaygps.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import se.railwaygps.server.RailwayGpsServer;
import se.railwaygps.storage.RailwayNetworkJsonCodec;
import se.railwaygps.railway.model.RailwayNetwork;

public final class RailwayGpsNetworking {
	private RailwayGpsNetworking() {
	}

	public static void register() {
		PayloadTypeRegistry.clientboundPlay().registerLarge(
				RailwayNetworkPayload.TYPE, RailwayNetworkPayload.CODEC, RailwayNetworkPayload.MAX_PAYLOAD_BYTES);
		PayloadTypeRegistry.clientboundPlay().register(SurveyStatusPayload.TYPE, SurveyStatusPayload.CODEC);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			RailwayGpsServer state = RailwayGpsServer.get(server);
			send(handler.getPlayer(), state.network());
		});
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
