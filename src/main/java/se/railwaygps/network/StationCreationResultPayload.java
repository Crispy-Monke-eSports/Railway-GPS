package se.railwaygps.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import se.railwaygps.RailwayGps;

public record StationCreationResultPayload(boolean success, String messageKey, String detail)
		implements CustomPacketPayload {
	public static final Type<StationCreationResultPayload> TYPE = new Type<>(
			Identifier.fromNamespaceAndPath(RailwayGps.MOD_ID, "station_creation_result"));
	public static final StreamCodec<RegistryFriendlyByteBuf, StationCreationResultPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL, StationCreationResultPayload::success,
			ByteBufCodecs.stringUtf8(160), StationCreationResultPayload::messageKey,
			ByteBufCodecs.stringUtf8(512), StationCreationResultPayload::detail,
			StationCreationResultPayload::new);
	@Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
