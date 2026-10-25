package se.railwaygps.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import se.railwaygps.RailwayGps;

/** Small live survey snapshot used by the visual administration workflow. */
public record SurveyStatusPayload(String state, String messageKey, String detail, int pointCount,
		int visitedControlPoints, double distance, long elapsedTicks) implements CustomPacketPayload {
	public static final Type<SurveyStatusPayload> TYPE = new Type<>(
			Identifier.fromNamespaceAndPath(RailwayGps.MOD_ID, "survey_status"));
	public static final StreamCodec<RegistryFriendlyByteBuf, SurveyStatusPayload> CODEC = new StreamCodec<>() {
		@Override
		public SurveyStatusPayload decode(RegistryFriendlyByteBuf buffer) {
			return new SurveyStatusPayload(buffer.readUtf(24), buffer.readUtf(160), buffer.readUtf(256),
					buffer.readVarInt(), buffer.readVarInt(), buffer.readDouble(), buffer.readVarLong());
		}

		@Override
		public void encode(RegistryFriendlyByteBuf buffer, SurveyStatusPayload payload) {
			buffer.writeUtf(payload.state(), 24);
			buffer.writeUtf(payload.messageKey(), 160);
			buffer.writeUtf(payload.detail(), 256);
			buffer.writeVarInt(payload.pointCount());
			buffer.writeVarInt(payload.visitedControlPoints());
			buffer.writeDouble(payload.distance());
			buffer.writeVarLong(payload.elapsedTicks());
		}
	};

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
