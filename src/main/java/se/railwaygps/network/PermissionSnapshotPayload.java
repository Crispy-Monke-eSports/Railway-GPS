package se.railwaygps.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import se.railwaygps.RailwayGps;
import se.railwaygps.permission.RailwayCapabilities;

public record PermissionSnapshotPayload(boolean providerControlled, boolean inspect, boolean name,
		boolean survey, boolean admin) implements CustomPacketPayload {
	public static final Type<PermissionSnapshotPayload> TYPE = new Type<>(
			Identifier.fromNamespaceAndPath(RailwayGps.MOD_ID, "permissions"));
	public static final StreamCodec<RegistryFriendlyByteBuf, PermissionSnapshotPayload> CODEC = StreamCodec.composite(
			ByteBufCodecs.BOOL, PermissionSnapshotPayload::providerControlled,
			ByteBufCodecs.BOOL, PermissionSnapshotPayload::inspect,
			ByteBufCodecs.BOOL, PermissionSnapshotPayload::name,
			ByteBufCodecs.BOOL, PermissionSnapshotPayload::survey,
			ByteBufCodecs.BOOL, PermissionSnapshotPayload::admin,
			PermissionSnapshotPayload::new);

	public static PermissionSnapshotPayload from(RailwayCapabilities capabilities) {
		return new PermissionSnapshotPayload(capabilities.providerControlled(), capabilities.inspect(),
				capabilities.name(), capabilities.survey(), capabilities.admin());
	}

	@Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
