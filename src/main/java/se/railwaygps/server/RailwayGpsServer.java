package se.railwaygps.server;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import se.railwaygps.RailwayGps;
import se.railwaygps.model.NetworkValidationException;
import se.railwaygps.model.RailwayNetwork;
import se.railwaygps.recording.RecordingManager;
import se.railwaygps.network.RailwayGpsNetworking;
import se.railwaygps.storage.NetworkStorage;
import se.railwaygps.storage.NetworkV2Storage;
import se.railwaygps.v2.model.RailwayNetworkV2;
import se.railwaygps.v2.naming.LabelSelectionManager;

import java.io.IOException;
import java.util.Map;
import java.util.WeakHashMap;

public final class RailwayGpsServer {
	private static final Map<MinecraftServer, RailwayGpsServer> INSTANCES = new WeakHashMap<>();

	private final NetworkStorage storage;
	private final NetworkV2Storage storageV2;
	private final RecordingManager recordings;
	private final LabelSelectionManager labelSelections;
	private final MinecraftServer server;
	private RailwayNetwork network = RailwayNetwork.EMPTY;
	private RailwayNetworkV2 networkV2 = RailwayNetworkV2.EMPTY;

	private RailwayGpsServer(MinecraftServer server) {
		this.server = server;
		this.storage = new NetworkStorage(server.getWorldPath(LevelResource.ROOT));
		this.storageV2 = new NetworkV2Storage(server.getWorldPath(LevelResource.ROOT));
		this.recordings = new RecordingManager(this);
		this.labelSelections = new LabelSelectionManager(this);
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
		try {
			state.reloadV2();
			RailwayGps.LOGGER.info("Loaded Railway GPS v2 network revision {}: {} control points, {} tracks, {} labels",
					state.networkV2.revision(), state.networkV2.controlPoints().size(), state.networkV2.tracks().size(),
					state.networkV2.labels().size());
		} catch (IOException | NetworkValidationException exception) {
			RailwayGps.LOGGER.error("Could not load Railway GPS v2 network from {}. Keeping an empty v2 network.",
					state.storageV2.file(), exception);
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

	public RailwayNetworkV2 networkV2() {
		return networkV2;
	}

	public LabelSelectionManager labelSelections() {
		return labelSelections;
	}

	public RailwayNetworkV2 reloadV2() throws IOException, NetworkValidationException {
		RailwayNetworkV2 candidate = storageV2.load();
		this.networkV2 = candidate;
		return candidate;
	}

	public void saveV2(RailwayNetworkV2 updated) throws IOException, NetworkValidationException {
		storageV2.save(updated);
		this.networkV2 = updated;
	}

	/** Loads into a temporary model and only swaps the active network after full validation. */
	public RailwayNetwork reload() throws IOException, NetworkValidationException {
		RailwayNetwork candidate = storage.load();
		this.network = candidate;
		RailwayGpsNetworking.broadcast(server, candidate);
		return candidate;
	}
}
