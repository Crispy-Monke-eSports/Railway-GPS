package se.railwaygps.client;

import se.railwaygps.model.RailwayNetwork;

public final class ClientRailwayNetwork {
	private static volatile RailwayNetwork network = RailwayNetwork.EMPTY;
	private static volatile String version = "";

	private ClientRailwayNetwork() {
	}

	public static RailwayNetwork network() {
		return network;
	}

	public static String version() {
		return version;
	}

	static void replace(String newVersion, RailwayNetwork newNetwork) {
		version = newVersion;
		network = newNetwork;
	}

	static void clear() {
		version = "";
		network = RailwayNetwork.EMPTY;
	}
}
