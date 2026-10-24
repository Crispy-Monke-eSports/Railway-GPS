package se.railwaygps.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import se.railwaygps.model.Junction;
import se.railwaygps.model.NetworkValidationException;
import se.railwaygps.model.NetworkValidator;
import se.railwaygps.model.RailwayNetwork;
import se.railwaygps.model.Route;
import se.railwaygps.model.Station;
import se.railwaygps.model.TrackPoint;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;

public final class NetworkStorage {
	private static final String JSON_GLOB = "*.json";

	private final Path root;
	private final Path stationsDirectory;
	private final Path junctionsDirectory;
	private final Path routesDirectory;
	private final Gson gson;

	public NetworkStorage(Path worldRoot) {
		this.root = worldRoot.resolve("railwaygps");
		this.stationsDirectory = root.resolve("stations");
		this.junctionsDirectory = root.resolve("junctions");
		this.routesDirectory = root.resolve("routes");
		this.gson = new GsonBuilder()
				.registerTypeAdapter(TrackPoint.class, new TrackPointAdapter())
				.setPrettyPrinting()
				.create();
	}

	public Path root() {
		return root;
	}

	public RailwayNetwork load() throws IOException, NetworkValidationException {
		ensureDirectories();
		Map<String, Station> stations = readDirectory(stationsDirectory, Station.class);
		Map<String, Junction> junctions = readDirectory(junctionsDirectory, Junction.class);
		Map<String, Route> routes = readDirectory(routesDirectory, Route.class);
		RailwayNetwork network = new RailwayNetwork(stations, junctions, routes);
		NetworkValidator.validate(network);
		return network;
	}

	public void createStation(Station station) throws IOException {
		writeNew(stationsDirectory, station.id(), station);
	}

	public void createJunction(Junction junction) throws IOException {
		writeNew(junctionsDirectory, junction.id(), junction);
	}

	public void createRoute(Route route) throws IOException {
		writeNew(routesDirectory, route.id(), route);
	}

	public void removeRoute(String id) throws IOException {
		validateId(id);
		Path route = routesDirectory.resolve(id + ".json");
		if (!Files.deleteIfExists(route)) {
			throw new IOException("Route '" + id + "' does not exist");
		}
	}

	public void renameRoute(Route existing, String newId) throws IOException {
		validateId(newId);
		Path oldPath = routesDirectory.resolve(existing.id() + ".json");
		if (!Files.exists(oldPath)) {
			throw new IOException("Route '" + existing.id() + "' does not exist");
		}
		Route renamed = new Route(newId, existing.name(), existing.from(), existing.to(), existing.dimension(),
				existing.bidirectional(), existing.distance(), existing.points());
		writeNew(routesDirectory, newId, renamed);
		try {
			Files.delete(oldPath);
		} catch (IOException exception) {
			Files.deleteIfExists(routesDirectory.resolve(newId + ".json"));
			throw exception;
		}
	}

	private void ensureDirectories() throws IOException {
		Files.createDirectories(stationsDirectory);
		Files.createDirectories(junctionsDirectory);
		Files.createDirectories(routesDirectory);
	}

	private <T> Map<String, T> readDirectory(Path directory, Class<T> type) throws IOException {
		Map<String, T> result = new LinkedHashMap<>();
		try (DirectoryStream<Path> files = Files.newDirectoryStream(directory, JSON_GLOB)) {
			for (Path path : files) {
				String id = stripJsonExtension(path.getFileName().toString());
				try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
					T value = gson.fromJson(reader, type);
					if (value == null) {
						throw new IOException("Empty JSON object in " + path);
					}
					result.put(id, value);
				} catch (JsonParseException exception) {
					throw new IOException("Could not parse " + path + ": " + exception.getMessage(), exception);
				}
			}
		}
		return result;
	}

	private void writeNew(Path directory, String id, Object value) throws IOException {
		validateId(id);
		ensureDirectories();
		Path destination = directory.resolve(id + ".json");
		if (Files.exists(destination)) {
			throw new IOException("Object '" + id + "' already exists");
		}

		Path temporary = Files.createTempFile(directory, id + ".", ".tmp");
		try {
			try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
				gson.toJson(value, writer);
			}
			moveAtomically(temporary, destination);
		} finally {
			Files.deleteIfExists(temporary);
		}
	}

	private static void validateId(String id) {
		if (!NetworkValidator.isValidId(id)) {
			throw new IllegalArgumentException("Invalid id '" + id + "'. Use lowercase letters, numbers, '_' or '-'.");
		}
	}

	private static void moveAtomically(Path source, Path destination) throws IOException {
		try {
			Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE);
		} catch (AtomicMoveNotSupportedException exception) {
			Files.move(source, destination);
		}
	}

	private static String stripJsonExtension(String name) {
		return name.substring(0, name.length() - ".json".length());
	}
}
