package se.railwaygps.network;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Shared SHA-256 digest of network snapshot JSON so sender and receiver agree on the same value. */
public final class NetworkSnapshotDigest {
	private static final int SHA256_HEX_LENGTH = 64;
	private static final HexFormat HEX = HexFormat.of();

	private NetworkSnapshotDigest() {
	}

	/** Returns the lowercase hexadecimal SHA-256 digest of the UTF-8 encoded JSON. */
	public static String of(String json) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(json.getBytes(StandardCharsets.UTF_8));
			return HEX.formatHex(digest);
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is unavailable", exception);
		}
	}

	/** Returns true when the advertised digest is well formed and matches the received JSON. */
	public static boolean verifies(String advertisedDigest, String json) {
		return advertisedDigest != null && isWellFormed(advertisedDigest)
				&& advertisedDigest.equalsIgnoreCase(of(json));
	}

	private static boolean isWellFormed(String digest) {
		if (digest.length() != SHA256_HEX_LENGTH) {
			return false;
		}
		for (int index = 0; index < digest.length(); index++) {
			char character = digest.charAt(index);
			if (!isHexDigit(character)) {
				return false;
			}
		}
		return true;
	}

	private static boolean isHexDigit(char character) {
		return (character >= '0' && character <= '9') || (character >= 'a' && character <= 'f')
				|| (character >= 'A' && character <= 'F');
	}
}
