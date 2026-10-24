package se.railwaygps.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import se.railwaygps.model.RailwayNetwork;
import se.railwaygps.server.RailwayGpsServer;
import se.railwaygps.storage.NetworkJsonCodec;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class RailwayGpsNetworking {
	private RailwayGpsNetworking() {
	}

	public static void register() {
		PayloadTypeRegistry.clientboundPlay().registerLarge(
				NetworkSnapshotPayload.TYPE, NetworkSnapshotPayload.CODEC, NetworkSnapshotPayload.MAX_PAYLOAD_BYTES);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
				send(handler.getPlayer(), RailwayGpsServer.get(server).network()));
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
		return new NetworkSnapshotPayload(version(json), json);
	}

	private static String version(String json) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(json.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is unavailable", exception);
		}
	}
}
