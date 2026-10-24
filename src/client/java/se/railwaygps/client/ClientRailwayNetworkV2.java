package se.railwaygps.client;

import se.railwaygps.v2.model.RailwayNetworkV2;

public final class ClientRailwayNetworkV2 {
	private static volatile RailwayNetworkV2 network = RailwayNetworkV2.EMPTY;
	private static volatile String version = "";

	private ClientRailwayNetworkV2() {
	}

	public static RailwayNetworkV2 network() {
		return network;
	}

	public static String version() {
		return version;
	}

	static void replace(String newVersion, RailwayNetworkV2 newNetwork) {
		version = newVersion;
		network = newNetwork;
	}

	static void clear() {
		version = "";
		network = RailwayNetworkV2.EMPTY;
	}
}
