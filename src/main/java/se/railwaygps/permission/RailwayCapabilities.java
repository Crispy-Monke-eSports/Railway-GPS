package se.railwaygps.permission;

import java.util.function.Predicate;

public record RailwayCapabilities(boolean providerControlled, boolean inspect, boolean name,
		boolean survey, boolean admin) {
	public static RailwayCapabilities resolve(boolean providerControlled, Predicate<RailwayPermission> allowed) {
		return new RailwayCapabilities(providerControlled, allowed.test(RailwayPermission.INSPECT),
				allowed.test(RailwayPermission.NAME), allowed.test(RailwayPermission.SURVEY),
				allowed.test(RailwayPermission.ADMIN));
	}

	public boolean anyTools() {
		return inspect || name || survey || admin;
	}
}
