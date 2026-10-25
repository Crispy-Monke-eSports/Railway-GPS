package se.railwaygps.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import se.railwaygps.RailwayGps;

public record TransitionTogglePayload(String transitionId, boolean enabled) implements CustomPacketPayload {
	public static final Type<TransitionTogglePayload> TYPE = new Type<>(
			Identifier.fromNamespaceAndPath(RailwayGps.MOD_ID, "transition_toggle"));
	public static final StreamCodec<RegistryFriendlyByteBuf, TransitionTogglePayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.stringUtf8(160), TransitionTogglePayload::transitionId,
			ByteBufCodecs.BOOL, TransitionTogglePayload::enabled,
			TransitionTogglePayload::new);
	@Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
