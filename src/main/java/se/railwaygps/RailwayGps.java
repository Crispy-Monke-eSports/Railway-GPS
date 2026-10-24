package se.railwaygps;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.railwaygps.command.RgpsCommands;
import se.railwaygps.network.RailwayGpsNetworking;
import se.railwaygps.server.RailwayGpsServer;

public final class RailwayGps implements ModInitializer {
	public static final String MOD_ID = "railwaygps";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		RgpsCommands.register();
		RailwayGpsNetworking.register();
		ServerLifecycleEvents.SERVER_STARTED.register(RailwayGpsServer::start);
		ServerLifecycleEvents.SERVER_STOPPED.register(RailwayGpsServer::stop);
		ServerTickEvents.END_SERVER_TICK.register(server -> RailwayGpsServer.get(server).recordings().tick(server));
		LOGGER.info("Railway GPS initialized");
	}
}
