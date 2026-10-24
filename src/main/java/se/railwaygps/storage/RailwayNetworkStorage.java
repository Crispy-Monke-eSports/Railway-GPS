package se.railwaygps.storage;

import se.railwaygps.model.NetworkValidationException;
import se.railwaygps.railway.model.RailwayNetwork;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Stores the authoritative physical railway model in one atomic world-local document. */
public final class RailwayNetworkStorage {
	private static final String FILE_NAME = "network.json";

	private final Path root;
	private final Path file;

	public RailwayNetworkStorage(Path worldRoot) {
		this.root = worldRoot.resolve("railwaygps");
		this.file = root.resolve(FILE_NAME);
	}

	public Path file() {
		return file;
	}

	public RailwayNetwork load() throws IOException, NetworkValidationException {
		Files.createDirectories(root);
		if (!Files.exists(file)) {
			return RailwayNetwork.EMPTY;
		}
		return RailwayNetworkJsonCodec.decode(Files.readString(file, StandardCharsets.UTF_8));
	}

	public void save(RailwayNetwork network) throws IOException, NetworkValidationException {
		String json = RailwayNetworkJsonCodec.encode(network);
		Files.createDirectories(root);
		Path temporary = Files.createTempFile(root, "network.", ".tmp");
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
