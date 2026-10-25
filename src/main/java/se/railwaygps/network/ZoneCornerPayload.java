package se.railwaygps.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import se.railwaygps.RailwayGps;

/** A block selected with the editor's mouse controls (no chat command required). */
public record ZoneCornerPayload(boolean first, int x, int y, int z) implements CustomPacketPayload {
	public static final Type<ZoneCornerPayload> TYPE = new Type<>(
			Identifier.fromNamespaceAndPath(RailwayGps.MOD_ID, "zone_corner"));
	public static final StreamCodec<RegistryFriendlyByteBuf, ZoneCornerPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL, ZoneCornerPayload::first,
			ByteBufCodecs.VAR_INT, ZoneCornerPayload::x,
			ByteBufCodecs.VAR_INT, ZoneCornerPayload::y,
			ByteBufCodecs.VAR_INT, ZoneCornerPayload::z,
			ZoneCornerPayload::new);
	@Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
