package se.railwaygps.railway.editing;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import se.railwaygps.model.TrackPoint;
import se.railwaygps.railway.model.ControlPoint;
import se.railwaygps.railway.model.Zone;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Per-player two-corner selections used to create control-point bounding boxes. */
public final class ZoneSelectionManager {
	private static final double DEFAULT_EXIT_MARGIN = 1.0;
	private final Map<UUID, Selection> selections = new HashMap<>();

	public Selection markFirst(ServerPlayer player) {
		return markFirst(player, player.blockPosition());
	}

	public Selection markFirst(ServerPlayer player, BlockPos position) {
		Selection previous = selections.get(player.getUUID());
		Selection updated = new Selection(dimension(player), position,
				previous != null && previous.dimension().equals(dimension(player)) ? previous.second() : null);
		selections.put(player.getUUID(), updated);
		return updated;
	}

	public Selection markSecond(ServerPlayer player) {
		return markSecond(player, player.blockPosition());
	}

	public Selection markSecond(ServerPlayer player, BlockPos position) {
		Selection previous = selections.get(player.getUUID());
		if (previous == null || previous.first() == null) {
			throw new IllegalStateException("Select the first zone corner first");
		}
		if (!previous.dimension().equals(dimension(player))) {
			throw new IllegalStateException("Both zone corners must be in the same dimension");
		}
		Selection updated = new Selection(previous.dimension(), previous.first(), position);
		selections.put(player.getUUID(), updated);
		return updated;
	}

	public ControlPoint create(ServerPlayer player, String id, String name, ControlPoint.Kind kind) {
		Selection selection = selections.get(player.getUUID());
		if (selection == null || selection.first() == null || selection.second() == null) {
			throw new IllegalStateException("Select both zone corners first");
		}
		BlockPos first = selection.first();
		BlockPos second = selection.second();
		TrackPoint min = new TrackPoint(Math.min(first.getX(), second.getX()), Math.min(first.getY(), second.getY()),
				Math.min(first.getZ(), second.getZ()));
		TrackPoint max = new TrackPoint(Math.max(first.getX(), second.getX()), Math.max(first.getY(), second.getY()),
				Math.max(first.getZ(), second.getZ()));
		return new ControlPoint(id, name, kind,
				new Zone(selection.dimension(), List.of(new se.railwaygps.railway.model.ZoneBox(min, max)), DEFAULT_EXIT_MARGIN),
				Map.of());
	}

	public void clear(ServerPlayer player) {
		selections.remove(player.getUUID());
	}

	private static String dimension(ServerPlayer player) {
		return player.level().dimension().identifier().toString();
	}

	public record Selection(String dimension, BlockPos first, BlockPos second) {
	}
}
