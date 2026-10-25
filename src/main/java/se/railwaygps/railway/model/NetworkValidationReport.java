package se.railwaygps.railway.model;

import java.util.List;

/** Object-specific blocking errors and non-blocking warnings suitable for GUI presentation. */
public record NetworkValidationReport(List<String> errors, List<String> warnings) {
	public NetworkValidationReport {
		errors = List.copyOf(errors);
		warnings = List.copyOf(warnings);
	}

	public boolean valid() { return errors.isEmpty(); }
}
