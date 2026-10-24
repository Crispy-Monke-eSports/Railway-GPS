package se.railwaygps.model;

import java.util.List;

public final class NetworkValidationException extends Exception {
	private final List<String> errors;

	public NetworkValidationException(List<String> errors) {
		super(String.join("; ", errors));
		this.errors = List.copyOf(errors);
	}

	public List<String> errors() {
		return errors;
	}
}
