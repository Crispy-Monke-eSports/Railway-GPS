package se.railwaygps.server;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import se.railwaygps.RailwayGps;
import se.railwaygps.model.NetworkValidationException;
import se.railwaygps.network.RailwayGpsNetworking;
import se.railwaygps.storage.RailwayNetworkStorage;
import se.railwaygps.railway.model.RailwayNetwork;
import se.railwaygps.railway.editing.SurveyManager;
import se.railwaygps.railway.editing.ZoneSelectionManager;
import se.railwaygps.railway.naming.LabelSelectionManager;

import java.io.IOException;
import java.util.Map;
import java.util.WeakHashMap;

public final class RailwayGpsServer {
	private static final Map<MinecraftServer, RailwayGpsServer> INSTANCES = new WeakHashMap<>();

	private final RailwayNetworkStorage storage;
	private final LabelSelectionManager labelSelections;
	private final ZoneSelectionManager zoneSelections;
	private final SurveyManager surveys;
	private final MinecraftServer server;
	private RailwayNetwork network = RailwayNetwork.EMPTY;

	private RailwayGpsServer(MinecraftServer server) {
		this.server = server;
		this.storage = new RailwayNetworkStorage(server.getWorldPath(LevelResource.ROOT));
		this.labelSelections = new LabelSelectionManager(this);
		this.zoneSelections = new ZoneSelectionManager();
		this.surveys = new SurveyManager(this);
	}

	public static synchronized RailwayGpsServer start(MinecraftServer server) {
		RailwayGpsServer state = new RailwayGpsServer(server);
		INSTANCES.put(server, state);
		try {
			state.reload();
			RailwayGps.LOGGER.info("Loaded Railway GPS network revision {}: {} control points, {} tracks, {} labels",
					state.network.revision(), state.network.controlPoints().size(), state.network.tracks().size(),
					state.network.labels().size());
		} catch (IOException | NetworkValidationException exception) {
			RailwayGps.LOGGER.error("Could not load Railway GPS network from {}. Keeping an empty network.",
					state.storage.file(), exception);
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

	public RailwayNetwork network() {
		return network;
	}

	public LabelSelectionManager labelSelections() {
		return labelSelections;
	}

	public ZoneSelectionManager zoneSelections() {
		return zoneSelections;
	}

	public SurveyManager surveys() {
		return surveys;
	}

	public RailwayNetwork reload() throws IOException, NetworkValidationException {
		RailwayNetwork candidate = storage.load();
		this.network = candidate;
		RailwayGpsNetworking.broadcast(server, candidate);
		return candidate;
	}

	public void save(RailwayNetwork updated) throws IOException, NetworkValidationException {
		storage.save(updated);
		this.network = updated;
		RailwayGpsNetworking.broadcast(server, updated);
	}

}
