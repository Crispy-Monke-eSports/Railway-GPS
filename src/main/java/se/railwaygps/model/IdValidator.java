package se.railwaygps.model;

import java.util.regex.Pattern;

public final class IdValidator {
	private static final Pattern SAFE_ID = Pattern.compile("[a-z0-9][a-z0-9_-]{0,63}");

	private IdValidator() { }

	public static boolean isValid(String id) {
		return id != null && SAFE_ID.matcher(id).matches();
	}
}
