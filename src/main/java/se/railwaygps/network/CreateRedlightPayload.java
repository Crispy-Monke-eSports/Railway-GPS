package se.railwaygps.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import se.railwaygps.RailwayGps;

/** Creates a redlight control point from the player's two selected zone corners. */
public record CreateRedlightPayload(String name, boolean acknowledgeOverlaps) implements CustomPacketPayload {
	public static final Type<CreateRedlightPayload> TYPE = new Type<>(
			Identifier.fromNamespaceAndPath(RailwayGps.MOD_ID, "create_redlight"));
	public static final StreamCodec<RegistryFriendlyByteBuf, CreateRedlightPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.stringUtf8(100), CreateRedlightPayload::name,
			ByteBufCodecs.BOOL, CreateRedlightPayload::acknowledgeOverlaps,
			CreateRedlightPayload::new);
	@Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
