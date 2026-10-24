package se.railwaygps.server;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import se.railwaygps.RailwayGps;
import se.railwaygps.model.NetworkValidationException;
import se.railwaygps.model.RailwayNetwork;
import se.railwaygps.recording.RecordingManager;
import se.railwaygps.storage.NetworkStorage;

import java.io.IOException;
import java.util.Map;
import java.util.WeakHashMap;

public final class RailwayGpsServer {
	private static final Map<MinecraftServer, RailwayGpsServer> INSTANCES = new WeakHashMap<>();

	private final NetworkStorage storage;
	private final RecordingManager recordings;
	private RailwayNetwork network = RailwayNetwork.EMPTY;

	private RailwayGpsServer(MinecraftServer server) {
		this.storage = new NetworkStorage(server.getWorldPath(LevelResource.ROOT));
		this.recordings = new RecordingManager(this);
	}

	public static synchronized RailwayGpsServer start(MinecraftServer server) {
		RailwayGpsServer state = new RailwayGpsServer(server);
		INSTANCES.put(server, state);
		try {
			state.reload();
			RailwayGps.LOGGER.info("Loaded Railway GPS network: {} stations, {} junctions, {} routes",
					state.network.stations().size(), state.network.junctions().size(), state.network.routes().size());
		} catch (IOException | NetworkValidationException exception) {
			RailwayGps.LOGGER.error("Could not load Railway GPS network from {}. Keeping an empty network.",
					state.storage.root(), exception);
		}
		return state;
	}

	public static synchronized RailwayGpsServer get(MinecraftServer server) {
		RailwayGpsServer state = INSTANCES.get(server);
		if (state == null) {
			throw new IllegalStateException("Railway GPS server state is not initialized");
		}
		return state;
	}

	public static synchronized void stop(MinecraftServer server) {
		INSTANCES.remove(server);
	}

	public NetworkStorage storage() {
		return storage;
	}

	public RailwayNetwork network() {
		return network;
	}

	public RecordingManager recordings() {
		return recordings;
	}

	/** Loads into a temporary model and only swaps the active network after full validation. */
	public RailwayNetwork reload() throws IOException, NetworkValidationException {
		RailwayNetwork candidate = storage.load();
		this.network = candidate;
		return candidate;
	}
}
