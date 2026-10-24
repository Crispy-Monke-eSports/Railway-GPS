package se.railwaygps.railway.naming;

import net.minecraft.server.level.ServerPlayer;
import se.railwaygps.model.NetworkValidationException;
import se.railwaygps.server.RailwayGpsServer;
import se.railwaygps.railway.model.RailwayNetwork;
import se.railwaygps.railway.model.TrackLabel;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class LabelSelectionManager {
	private static final double SNAP_DISTANCE = 8.0;

	private final RailwayGpsServer state;
	private final Map<UUID, Session> sessions = new HashMap<>();

	public LabelSelectionManager(RailwayGpsServer state) {
		this.state = state;
	}

	public TrackPathSelector.Point markStart(ServerPlayer player) {
		TrackPathSelector.Point point = point(player);
		sessions.put(player.getUUID(), new Session(point, null, null, state.network().revision()));
		return point;
	}

	public TrackLabelEditor.Preview markEnd(ServerPlayer player) {
		Session session = requireSession(player);
		TrackPathSelector.Point end = point(player);
		RailwayNetwork network = state.network();
		TrackLabelEditor.Preview preview = TrackLabelEditor.preview(network, session.start(), end, SNAP_DISTANCE);
		sessions.put(player.getUUID(), new Session(session.start(), end, preview, network.revision()));
		return preview;
	}

	public Session session(ServerPlayer player) {
		return requireSession(player);
	}

	public RailwayNetwork create(
			ServerPlayer player,
			String id,
			String name,
			TrackLabel.Type type,
			int candidateIndex,
			boolean includePaired
	) throws NetworkValidationException, IOException {
		Session session = requireSession(player);
		if (session.preview() == null || !session.preview().successful()) {
			throw new IllegalStateException("Select an end point with a valid preview first");
		}
		if (session.networkRevision() != state.network().revision()) {
			throw new IllegalStateException("The network changed; select both points again");
		}
		RailwayNetwork updated = TrackLabelEditor.apply(state.network(), id, name, type, 0, null,
				session.preview(), candidateIndex, includePaired);
		state.save(updated);
		sessions.remove(player.getUUID());
		return updated;
	}

	public void clear(ServerPlayer player) {
		sessions.remove(player.getUUID());
	}

	private Session requireSession(ServerPlayer player) {
		Session session = sessions.get(player.getUUID());
		if (session == null) {
			throw new IllegalStateException("Select the start point first");
		}
		return session;
	}

	private static TrackPathSelector.Point point(ServerPlayer player) {
		return new TrackPathSelector.Point(
				player.level().dimension().identifier().toString(), player.getX(), player.getY(), player.getZ());
	}

	public record Session(
			TrackPathSelector.Point start,
			TrackPathSelector.Point end,
			TrackLabelEditor.Preview preview,
			long networkRevision
	) {
	}
}
