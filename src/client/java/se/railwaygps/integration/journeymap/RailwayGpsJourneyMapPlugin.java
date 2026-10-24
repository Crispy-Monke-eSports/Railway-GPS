package se.railwaygps.integration.journeymap;

import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.client.IClientPlugin;
import journeymap.api.v2.common.JourneyMapPlugin;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import se.railwaygps.RailwayGps;
import se.railwaygps.client.ClientRailwayNetwork;
import se.railwaygps.client.ClientRailwayNetworkV2;
import se.railwaygps.client.navigation.ClientNavigator;

/** Loaded exclusively by JourneyMap's optional Fabric entrypoint. */
@JourneyMapPlugin(apiVersion = "2.0.0")
public final class RailwayGpsJourneyMapPlugin implements IClientPlugin {
	private JourneyMapOverlayController overlays;
	private String observedNetworkVersion;
	private String observedNetworkV2Version;
	private long observedNavigationRevision = -1;

	@Override
	public void initialize(IClientAPI api) {
		this.overlays = new JourneyMapOverlayController(api);
		ClientTickEvents.END_CLIENT_TICK.register(client -> refreshWhenChanged());
		RailwayGps.LOGGER.info("JourneyMap integration initialized");
	}

	@Override
	public String getModId() {
		return RailwayGps.MOD_ID;
	}

	private void refreshWhenChanged() {
		String version = ClientRailwayNetwork.version();
		String versionV2 = ClientRailwayNetworkV2.version();
		long navigationRevision = ClientNavigator.revision();
		if (version.equals(observedNetworkVersion) && versionV2.equals(observedNetworkV2Version)
				&& navigationRevision == observedNavigationRevision) {
			return;
		}
		observedNetworkVersion = version;
		observedNetworkV2Version = versionV2;
		observedNavigationRevision = navigationRevision;
		overlays.refresh(ClientRailwayNetwork.network(), ClientRailwayNetworkV2.network(),
				ClientNavigator.activePlan(), ClientNavigator.currentLegIndex());
	}
}
