package se.railwaygps.permission;

import java.util.Optional;

/** Pure authorization policy: a provider decision is authoritative; op is fallback only. */
public final class PermissionPolicy {
	private PermissionPolicy() { }

	public static boolean authorize(Optional<Boolean> providerDecision, boolean operator) {
		return providerDecision.orElse(operator);
	}
}
