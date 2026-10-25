package se.railwaygps.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import se.railwaygps.RailwayGps;

public record TransitionToggleResultPayload(boolean success, String detail) implements CustomPacketPayload {
	public static final Type<TransitionToggleResultPayload> TYPE = new Type<>(
			Identifier.fromNamespaceAndPath(RailwayGps.MOD_ID, "transition_toggle_result"));
	public static final StreamCodec<RegistryFriendlyByteBuf, TransitionToggleResultPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL, TransitionToggleResultPayload::success,
			ByteBufCodecs.stringUtf8(512), TransitionToggleResultPayload::detail,
			TransitionToggleResultPayload::new);
	@Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
