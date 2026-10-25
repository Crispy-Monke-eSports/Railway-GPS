package se.railwaygps.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import se.railwaygps.RailwayGps;

public record TransitionInstructionPayload(String transitionId, String instruction) implements CustomPacketPayload {
	public static final Type<TransitionInstructionPayload> TYPE = new Type<>(
			Identifier.fromNamespaceAndPath(RailwayGps.MOD_ID, "transition_instruction"));
	public static final StreamCodec<RegistryFriendlyByteBuf, TransitionInstructionPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.stringUtf8(160), TransitionInstructionPayload::transitionId,
			ByteBufCodecs.stringUtf8(300), TransitionInstructionPayload::instruction,
			TransitionInstructionPayload::new);
	@Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
