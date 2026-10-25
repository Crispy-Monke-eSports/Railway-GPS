package se.railwaygps.client;

import se.railwaygps.network.StationCreationResultPayload;

public final class ClientStationCreationResult {
	private static StationCreationResultPayload result;
	private static long revision;
	private ClientStationCreationResult() { }
	public static StationCreationResultPayload get() { return result; }
	public static long revision() { return revision; }
	public static void replace(StationCreationResultPayload next) { result = next; revision++; }
	public static void clear() { result = null; revision++; }
}
