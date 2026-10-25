package se.railwaygps.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import se.railwaygps.RailwayGps;

public record CreateStationPayload(String name) implements CustomPacketPayload {
	public static final Type<CreateStationPayload> TYPE = new Type<>(
			Identifier.fromNamespaceAndPath(RailwayGps.MOD_ID, "create_station"));
	public static final StreamCodec<RegistryFriendlyByteBuf, CreateStationPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.stringUtf8(100), CreateStationPayload::name, CreateStationPayload::new);
	@Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
