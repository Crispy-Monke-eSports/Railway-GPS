package se.railwaygps.server;

import se.railwaygps.model.NetworkValidationException;
import se.railwaygps.railway.model.RailwayNetwork;
import se.railwaygps.railway.model.RailwayNetworkValidator;

import java.io.IOException;

/** Commits persistence before swapping and synchronizing the active immutable revision. */
public final class RailwayNetworkPublication {
	private final Store store;
	private final Synchronizer synchronizer;
	private RailwayNetwork active;

	public RailwayNetworkPublication(RailwayNetwork initial, Store store, Synchronizer synchronizer) {
		this.active = initial;
		this.store = store;
		this.synchronizer = synchronizer;
	}

	public RailwayNetwork active() {
		return active;
	}

	public void publish(RailwayNetwork candidate) throws IOException, NetworkValidationException {
		RailwayNetworkValidator.validate(candidate);
		store.save(candidate);
		active = candidate;
		synchronizer.synchronize(candidate);
	}

	public void activateLoaded(RailwayNetwork candidate) throws NetworkValidationException {
		RailwayNetworkValidator.validate(candidate);
		active = candidate;
		synchronizer.synchronize(candidate);
	}

	@FunctionalInterface
	public interface Store {
		void save(RailwayNetwork network) throws IOException, NetworkValidationException;
	}

	@FunctionalInterface
	public interface Synchronizer {
		void synchronize(RailwayNetwork network);
	}
}
