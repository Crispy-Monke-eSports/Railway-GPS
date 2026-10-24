package se.railwaygps.storage;

import se.railwaygps.model.NetworkValidationException;
import se.railwaygps.v2.model.RailwayNetworkV2;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Stores only the new v2 model. Legacy directories are intentionally never inspected or migrated. */
public final class NetworkV2Storage {
	private static final String FILE_NAME = "network-v2.json";

	private final Path root;
	private final Path file;

	public NetworkV2Storage(Path worldRoot) {
		this.root = worldRoot.resolve("railwaygps");
		this.file = root.resolve(FILE_NAME);
	}

	public Path file() {
		return file;
	}

	public RailwayNetworkV2 load() throws IOException, NetworkValidationException {
		Files.createDirectories(root);
		if (!Files.exists(file)) {
			return RailwayNetworkV2.EMPTY;
		}
		return NetworkV2JsonCodec.decode(Files.readString(file, StandardCharsets.UTF_8));
	}

	public void save(RailwayNetworkV2 network) throws IOException, NetworkValidationException {
		String json = NetworkV2JsonCodec.encode(network);
		Files.createDirectories(root);
		Path temporary = Files.createTempFile(root, "network-v2.", ".tmp");
		try {
			Files.writeString(temporary, json, StandardCharsets.UTF_8);
			try {
				Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			} catch (AtomicMoveNotSupportedException exception) {
				Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
			}
		} finally {
			Files.deleteIfExists(temporary);
		}
	}
}
