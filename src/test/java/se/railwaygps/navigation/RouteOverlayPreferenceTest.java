package se.railwaygps.navigation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RouteOverlayPreferenceTest {
	@TempDir Path directory;

	@Test void defaultsOffAndPersistsBothChoices() throws Exception {
		Path file = directory.resolve("railwaygps-route-overlay.txt");
		assertFalse(RouteOverlayPreference.load(file));
		RouteOverlayPreference.save(file, true);
		assertTrue(RouteOverlayPreference.load(file));
		RouteOverlayPreference.save(file, false);
		assertFalse(RouteOverlayPreference.load(file));
	}
}
