package se.railwaygps.railway.model;

/** Stateful enter/exit confirmation that suppresses control-zone boundary jitter. */
public final class ZoneOccupancyTracker {
	private boolean inside;
	private int consecutive;

	public Event observe(Zone zone, double x, double y, double z) {
		if (!inside) {
			if (!zone.contains(x, y, z)) {
				consecutive = 0;
				return Event.NONE;
			}
			if (++consecutive < zone.confirmationTicks()) return Event.NONE;
			inside = true;
			consecutive = 0;
			return Event.ENTERED;
		}
		if (zone.containsForExit(x, y, z)) {
			consecutive = 0;
			return Event.NONE;
		}
		if (++consecutive < zone.confirmationTicks()) return Event.NONE;
		inside = false;
		consecutive = 0;
		return Event.EXITED;
	}

	public boolean inside() { return inside; }
	public enum Event { NONE, ENTERED, EXITED }
}
