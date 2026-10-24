package se.railwaygps.recording;

public final class RecordingException extends Exception {
	public RecordingException(String message) {
		super(message);
	}

	public RecordingException(String message, Throwable cause) {
		super(message, cause);
	}
}
