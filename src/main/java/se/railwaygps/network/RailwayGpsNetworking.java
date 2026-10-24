package se.railwaygps.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import se.railwaygps.model.RailwayNetwork;
import se.railwaygps.server.RailwayGpsServer;
import se.railwaygps.storage.NetworkJsonCodec;
import se.railwaygps.storage.NetworkV2JsonCodec;
import se.railwaygps.v2.model.RailwayNetworkV2;

public final class RailwayGpsNetworking {
	private RailwayGpsNetworking() {
	}

	public static void register() {
		PayloadTypeRegistry.clientboundPlay().registerLarge(
				NetworkSnapshotPayload.TYPE, NetworkSnapshotPayload.CODEC, NetworkSnapshotPayload.MAX_PAYLOAD_BYTES);
		PayloadTypeRegistry.clientboundPlay().registerLarge(
				NetworkV2SnapshotPayload.TYPE, NetworkV2SnapshotPayload.CODEC, NetworkV2SnapshotPayload.MAX_PAYLOAD_BYTES);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			RailwayGpsServer state = RailwayGpsServer.get(server);
			send(handler.getPlayer(), state.network());
			sendV2(handler.getPlayer(), state.networkV2());
		});
	}

	public static void broadcastV2(MinecraftServer server, RailwayNetworkV2 network) {
		NetworkV2SnapshotPayload payload = createV2Payload(network);
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (ServerPlayNetworking.canSend(player, NetworkV2SnapshotPayload.TYPE)) {
				ServerPlayNetworking.send(player, payload);
			}
		}
	}

	public static void broadcast(MinecraftServer server, RailwayNetwork network) {
		NetworkSnapshotPayload payload = createPayload(network);
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (ServerPlayNetworking.canSend(player, NetworkSnapshotPayload.TYPE)) {
				ServerPlayNetworking.send(player, payload);
			}
		}
	}

	private static void send(ServerPlayer player, RailwayNetwork network) {
		if (ServerPlayNetworking.canSend(player, NetworkSnapshotPayload.TYPE)) {
			ServerPlayNetworking.send(player, createPayload(network));
		}
	}

	private static NetworkSnapshotPayload createPayload(RailwayNetwork network) {
		String json = NetworkJsonCodec.encode(network);
		return new NetworkSnapshotPayload(NetworkSnapshotDigest.of(json), json);
	}

	private static void sendV2(ServerPlayer player, RailwayNetworkV2 network) {
		if (ServerPlayNetworking.canSend(player, NetworkV2SnapshotPayload.TYPE)) {
			ServerPlayNetworking.send(player, createV2Payload(network));
		}
	}

	private static NetworkV2SnapshotPayload createV2Payload(RailwayNetworkV2 network) {
		try {
			String json = NetworkV2JsonCodec.encode(network);
			return new NetworkV2SnapshotPayload(NetworkSnapshotDigest.of(json), json);
		} catch (se.railwaygps.model.NetworkValidationException exception) {
			throw new IllegalStateException("Cannot send an invalid Railway GPS v2 network", exception);
		}
	}
}
