package se.railwaygps.storage;

import se.railwaygps.model.NetworkValidationException;
import se.railwaygps.railway.model.RailwayNetwork;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

/** Stores the authoritative physical railway model in one atomic world-local document. */
public final class RailwayNetworkStorage {
	private static final String FILE_NAME = "network.json";

	private final Path root;
	private final Path file;
	private final BeforeCommit beforeCommit;
	/** Prevent an unloaded or corrupt saved network from being replaced by an empty revision. */
	private boolean loadFailed;

	public RailwayNetworkStorage(Path worldRoot) {
		this(worldRoot, (temporary, destination) -> { });
	}

	RailwayNetworkStorage(Path worldRoot, BeforeCommit beforeCommit) {
		this.root = worldRoot.resolve("railwaygps");
		this.file = root.resolve(FILE_NAME);
		this.beforeCommit = beforeCommit;
	}

	public Path file() {
		return file;
	}

	public RailwayNetwork load() throws IOException, NetworkValidationException {
		Files.createDirectories(root);
		if (!Files.exists(file)) {
			loadFailed = false;
			return RailwayNetwork.EMPTY;
		}
		try {
			RailwayNetwork loaded = RailwayNetworkJsonCodec.decode(Files.readString(file, StandardCharsets.UTF_8));
			loadFailed = false;
			return loaded;
		} catch (IOException | NetworkValidationException exception) {
			loadFailed = true;
			throw exception;
		}
	}

	public void save(RailwayNetwork network) throws IOException, NetworkValidationException {
		if (loadFailed) {
			throw new IOException("Railway GPS network was not loaded; repair or restore the original "
					+ "network.json and reload before saving. The existing data has been preserved.");
		}
		String json = RailwayNetworkJsonCodec.encode(network);
		Files.createDirectories(root);
		Path temporary = Files.createTempFile(root, "network.", ".tmp");
		try {
			Files.writeString(temporary, json, StandardCharsets.UTF_8);
			try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
				channel.force(true);
			}
			beforeCommit.run(temporary, file);
			Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
		} finally {
			Files.deleteIfExists(temporary);
		}
	}

	@FunctionalInterface
	interface BeforeCommit {
		void run(Path temporary, Path destination) throws IOException;
	}
}
