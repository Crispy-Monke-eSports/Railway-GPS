package se.railwaygps.navigation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Tiny local boolean setting; a missing or malformed file safely means HUD-only. */
public final class RouteOverlayPreference {
	private RouteOverlayPreference() { }

	public static boolean load(Path file) {
		try {
			return Files.exists(file) && "true".equalsIgnoreCase(Files.readString(file).trim());
		} catch (IOException exception) {
			return false;
		}
	}

	public static void save(Path file, boolean enabled) throws IOException {
		Files.createDirectories(file.getParent());
		Files.writeString(file, Boolean.toString(enabled) + "\n", StandardCharsets.UTF_8);
	}
}
