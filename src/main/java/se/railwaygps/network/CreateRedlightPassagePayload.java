package se.railwaygps.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import se.railwaygps.RailwayGps;

/** The client chooses verified track directions; the server validates and assigns the passage ID. */
public record CreateRedlightPassagePayload(
		String redlightId, String incomingTrackId, String incomingDirection,
		String outgoingTrackId, String outgoingDirection, String instruction) implements CustomPacketPayload {
	public static final Type<CreateRedlightPassagePayload> TYPE = new Type<>(
			Identifier.fromNamespaceAndPath(RailwayGps.MOD_ID, "create_redlight_passage"));
	public static final StreamCodec<RegistryFriendlyByteBuf, CreateRedlightPassagePayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.stringUtf8(160), CreateRedlightPassagePayload::redlightId,
			ByteBufCodecs.stringUtf8(160), CreateRedlightPassagePayload::incomingTrackId,
			ByteBufCodecs.stringUtf8(16), CreateRedlightPassagePayload::incomingDirection,
			ByteBufCodecs.stringUtf8(160), CreateRedlightPassagePayload::outgoingTrackId,
			ByteBufCodecs.stringUtf8(16), CreateRedlightPassagePayload::outgoingDirection,
			ByteBufCodecs.stringUtf8(300), CreateRedlightPassagePayload::instruction,
			CreateRedlightPassagePayload::new);
	@Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
