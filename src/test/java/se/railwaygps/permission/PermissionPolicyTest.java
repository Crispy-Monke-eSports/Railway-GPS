package se.railwaygps.permission;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class PermissionPolicyTest {
	@Test void providerGrantAllowsNonOperator() {
		assertTrue(PermissionPolicy.authorize(Optional.of(true), false));
	}

	@Test void providerDenialCannotBeBypassedByOperator() {
		assertFalse(PermissionPolicy.authorize(Optional.of(false), true));
	}

	@Test void absentProviderUsesOnlyOperatorFallback() {
		assertTrue(PermissionPolicy.authorize(Optional.empty(), true));
		assertFalse(PermissionPolicy.authorize(Optional.empty(), false));
	}

	@Test void capabilityNodesAreIndependentAndUnique() {
		assertEquals(4, java.util.Arrays.stream(RailwayPermission.values()).map(RailwayPermission::node).distinct().count());
	}
}
