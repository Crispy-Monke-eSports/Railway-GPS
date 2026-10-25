package se.railwaygps.permission;

import se.railwaygps.RailwayGps;

/** Independent server-controlled Railway GPS tool capabilities. */
public enum RailwayPermission {
	INSPECT("inspect"),
	NAME("name"),
	SURVEY("survey"),
	ADMIN("admin");

	private final String node;

	RailwayPermission(String node) {
		this.node = RailwayGps.MOD_ID + "." + node;
	}

	public String node() {
		return node;
	}
}
