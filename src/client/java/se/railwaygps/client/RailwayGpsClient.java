package se.railwaygps.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import se.railwaygps.RailwayGps;
import se.railwaygps.client.command.NavigationCommands;
import se.railwaygps.client.command.InspectorCommands;
import se.railwaygps.client.hud.NavigationHud;
import se.railwaygps.client.inspector.RailwayWorldOverlay;
import se.railwaygps.client.navigation.ClientNavigator;
import se.railwaygps.model.NetworkValidationException;
import se.railwaygps.network.NetworkSnapshotDigest;
import se.railwaygps.network.RailwayNetworkPayload;
import se.railwaygps.storage.RailwayNetworkJsonCodec;
import se.railwaygps.railway.model.RailwayNetwork;

/** Client entry point reserved for network synchronization and the navigation HUD. */
public final class RailwayGpsClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		NavigationCommands.register();
		InspectorCommands.register();
		NavigationHud.register();
		RailwayWorldOverlay.register();
		ClientTickEvents.END_CLIENT_TICK.register(ClientNavigator::tick);
		ClientPlayNetworking.registerGlobalReceiver(RailwayNetworkPayload.TYPE, (payload, context) -> {
			try {
				if (!NetworkSnapshotDigest.verifies(payload.version(), payload.json())) {
					RailwayGps.LOGGER.error("Rejected Railway GPS network snapshot with an invalid digest");
					return;
				}
				RailwayNetwork network = RailwayNetworkJsonCodec.decode(payload.json());
				ClientRailwayNetwork.replace(payload.version(), network);
				RailwayGps.LOGGER.info("Received Railway GPS revision {}: {} control points, {} tracks, {} labels",
						network.revision(), network.controlPoints().size(), network.tracks().size(), network.labels().size());
			} catch (NetworkValidationException exception) {
				RailwayGps.LOGGER.error("Rejected invalid Railway GPS snapshot; keeping the previous network", exception);
			}
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			ClientNavigator.stop();
			ClientRailwayNetwork.clear();
		});
	}
}
