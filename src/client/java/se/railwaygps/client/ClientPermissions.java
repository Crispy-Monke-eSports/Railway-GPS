package se.railwaygps.client;

import se.railwaygps.client.editing.RailwayEditor;
import se.railwaygps.client.inspector.RailwayWorldOverlay;
import se.railwaygps.network.PermissionSnapshotPayload;

public final class ClientPermissions {
	private static PermissionSnapshotPayload current = new PermissionSnapshotPayload(false, false, false, false, false);

	private ClientPermissions() { }

	public static PermissionSnapshotPayload get() { return current; }
	public static void replace(PermissionSnapshotPayload snapshot) {
		current = snapshot;
		if (!snapshot.inspect()) RailwayWorldOverlay.disableInspector();
		if (!snapshot.admin()) RailwayEditor.clear();
	}
	public static void clear() { current = new PermissionSnapshotPayload(false, false, false, false, false); }
	public static boolean anyTools() { return current.inspect() || current.name() || current.survey() || current.admin(); }
}
