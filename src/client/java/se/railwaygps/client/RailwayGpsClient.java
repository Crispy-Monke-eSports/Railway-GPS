package se.railwaygps.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import se.railwaygps.RailwayGps;
import se.railwaygps.model.NetworkValidationException;
import se.railwaygps.model.RailwayNetwork;
import se.railwaygps.network.NetworkSnapshotPayload;
import se.railwaygps.storage.NetworkJsonCodec;

/** Client entry point reserved for network synchronization and the navigation HUD. */
public final class RailwayGpsClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(NetworkSnapshotPayload.TYPE, (payload, context) -> {
			try {
				RailwayNetwork network = NetworkJsonCodec.decode(payload.json());
				ClientRailwayNetwork.replace(payload.version(), network);
				RailwayGps.LOGGER.info("Received Railway GPS network {}: {} stations, {} junctions, {} routes",
						payload.version(), network.stations().size(), network.junctions().size(), network.routes().size());
			} catch (NetworkValidationException exception) {
				RailwayGps.LOGGER.error("Rejected invalid Railway GPS network snapshot; keeping the previous network", exception);
			}
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientRailwayNetwork.clear());
	}
}
