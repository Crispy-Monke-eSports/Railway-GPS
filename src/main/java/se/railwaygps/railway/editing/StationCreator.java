package se.railwaygps.railway.editing;

import net.minecraft.server.level.ServerPlayer;
import se.railwaygps.model.NetworkValidationException;
import se.railwaygps.railway.model.ControlPoint;
import se.railwaygps.server.RailwayGpsServer;

import java.io.IOException;
import java.util.UUID;

/** Shared validated operation used by the primary GUI and optional commands. */
public final class StationCreator {
	private StationCreator() { }

	public static ControlPoint create(RailwayGpsServer state, ServerPlayer player, String name, boolean acknowledgeOverlaps)
			throws IOException, NetworkValidationException {
		if (name == null || name.trim().isEmpty()) throw new IllegalArgumentException("Enter a station name");
		String id = "station-" + UUID.randomUUID().toString().replace("-", "");
		ControlPoint station = state.zoneSelections().create(player, id, name.trim(), ControlPoint.Kind.STATION);
		state.save(RailwayNetworkEditor.addControlPoint(state.network(), id, station.name(), station.kind(),
				station.zone(), acknowledgeOverlaps));
		state.zoneSelections().clear(player);
		return station;
	}
}
