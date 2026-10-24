package se.railwaygps.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import se.railwaygps.RailwayGps;

public record NetworkV2SnapshotPayload(String version, String json) implements CustomPacketPayload {
	public static final int MAX_PAYLOAD_BYTES = 8 * 1024 * 1024;
	public static final Type<NetworkV2SnapshotPayload> TYPE = new Type<>(
			Identifier.fromNamespaceAndPath(RailwayGps.MOD_ID, "network_v2_snapshot"));
	public static final StreamCodec<RegistryFriendlyByteBuf, NetworkV2SnapshotPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.stringUtf8(128), NetworkV2SnapshotPayload::version,
			ByteBufCodecs.stringUtf8(MAX_PAYLOAD_BYTES), NetworkV2SnapshotPayload::json,
			NetworkV2SnapshotPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
