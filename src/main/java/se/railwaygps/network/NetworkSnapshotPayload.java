package se.railwaygps.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import se.railwaygps.RailwayGps;

public record NetworkSnapshotPayload(String version, String json) implements CustomPacketPayload {
	public static final int MAX_PAYLOAD_BYTES = 8 * 1024 * 1024;
	public static final Type<NetworkSnapshotPayload> TYPE = new Type<>(
			Identifier.fromNamespaceAndPath(RailwayGps.MOD_ID, "network_snapshot"));
	public static final StreamCodec<RegistryFriendlyByteBuf, NetworkSnapshotPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.stringUtf8(128), NetworkSnapshotPayload::version,
			ByteBufCodecs.stringUtf8(MAX_PAYLOAD_BYTES), NetworkSnapshotPayload::json,
			NetworkSnapshotPayload::new);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
