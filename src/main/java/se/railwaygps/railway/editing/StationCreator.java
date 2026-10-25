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
		return createControlPoint(state, player, name, ControlPoint.Kind.STATION, acknowledgeOverlaps);
	}

	public static ControlPoint createRedlight(RailwayGpsServer state, ServerPlayer player, String name,
			boolean acknowledgeOverlaps) throws IOException, NetworkValidationException {
		return createControlPoint(state, player, name, ControlPoint.Kind.REDLIGHT, acknowledgeOverlaps);
	}

	private static ControlPoint createControlPoint(RailwayGpsServer state, ServerPlayer player, String name,
			ControlPoint.Kind kind, boolean acknowledgeOverlaps) throws IOException, NetworkValidationException {
		if (name == null || name.trim().isEmpty()) throw new IllegalArgumentException("Enter a control point name");
		String prefix = kind == ControlPoint.Kind.REDLIGHT ? "redlight-" : "station-";
		String id = prefix + UUID.randomUUID().toString().replace("-", "");
		ControlPoint point = state.zoneSelections().create(player, id, name.trim(), kind);
		state.save(RailwayNetworkEditor.addControlPoint(state.network(), id, point.name(), point.kind(),
				point.zone(), acknowledgeOverlaps));
		state.zoneSelections().clear(player);
		return point;
	}
}
