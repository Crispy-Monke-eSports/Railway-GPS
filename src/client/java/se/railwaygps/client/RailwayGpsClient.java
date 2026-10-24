package se.railwaygps.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import se.railwaygps.RailwayGps;
import se.railwaygps.client.command.NavigationCommands;
import se.railwaygps.client.hud.NavigationHud;
import se.railwaygps.client.navigation.ClientNavigator;
import se.railwaygps.model.NetworkValidationException;
import se.railwaygps.model.RailwayNetwork;
import se.railwaygps.network.NetworkSnapshotDigest;
import se.railwaygps.network.NetworkSnapshotPayload;
import se.railwaygps.network.NetworkV2SnapshotPayload;
import se.railwaygps.storage.NetworkJsonCodec;
import se.railwaygps.storage.NetworkV2JsonCodec;
import se.railwaygps.v2.model.RailwayNetworkV2;

/** Client entry point reserved for network synchronization and the navigation HUD. */
public final class RailwayGpsClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		NavigationCommands.register();
		NavigationHud.register();
		ClientTickEvents.END_CLIENT_TICK.register(ClientNavigator::tick);
		ClientPlayNetworking.registerGlobalReceiver(NetworkSnapshotPayload.TYPE, (payload, context) -> {
			try {
				if (!NetworkSnapshotDigest.verifies(payload.version(), payload.json())) {
					RailwayGps.LOGGER.error(
							"Rejected Railway GPS network snapshot: advertised digest '{}' does not match the received JSON; keeping the previous network",
							payload.version());
					return;
				}
				RailwayNetwork network = NetworkJsonCodec.decode(payload.json());
				ClientRailwayNetwork.replace(payload.version(), network);
				RailwayGps.LOGGER.info("Received Railway GPS network {}: {} stations, {} junctions, {} routes",
						payload.version(), network.stations().size(), network.junctions().size(), network.routes().size());
			} catch (NetworkValidationException exception) {
				RailwayGps.LOGGER.error("Rejected invalid Railway GPS network snapshot; keeping the previous network", exception);
			}
		});
		ClientPlayNetworking.registerGlobalReceiver(NetworkV2SnapshotPayload.TYPE, (payload, context) -> {
			try {
				if (!NetworkSnapshotDigest.verifies(payload.version(), payload.json())) {
					RailwayGps.LOGGER.error("Rejected Railway GPS v2 snapshot with an invalid digest");
					return;
				}
				RailwayNetworkV2 network = NetworkV2JsonCodec.decode(payload.json());
				ClientRailwayNetworkV2.replace(payload.version(), network);
				RailwayGps.LOGGER.info("Received Railway GPS v2 revision {}: {} control points, {} tracks, {} labels",
						network.revision(), network.controlPoints().size(), network.tracks().size(), network.labels().size());
			} catch (NetworkValidationException exception) {
				RailwayGps.LOGGER.error("Rejected invalid Railway GPS v2 snapshot; keeping the previous network", exception);
			}
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			ClientNavigator.stop();
			ClientRailwayNetwork.clear();
			ClientRailwayNetworkV2.clear();
		});
	}
}
