# Railway GPS

Railway GPS is a Fabric mod for non-invasive navigation on large vanilla Minecraft railway networks. Administrators register stations and junctions, record actual minecart paths, and clients use that server-maintained graph for route guidance.

This repository currently contains the first Phase 1 foundation:

- Fabric 26.3 project with separated common and client source sets.
- World-local `railwaygps/stations`, `junctions`, and `routes` JSON storage.
- Immutable station, junction, route, and network models.
- Whole-network validation and safe reload that retains the last valid in-memory network.
- Atomic creation of individual JSON files without accidental overwrite.
- Operator-only `/rgps station add <id>`, `/rgps node add <id>`, `/rgps network list`, and `/rgps reload` commands.
- Server-side minecart route recording bound to one administrator and one specific minecart.
- Strict recording cancellation on dismount, cart swap/destruction, death, disconnect, or dimension change.
- Rail-block sampling, travelled-distance measurement, and conservative straight-line geometry simplification.
- Dijkstra shortest-path planning with directed, bidirectional, and parallel route support.
- Versioned server-to-client network snapshots, sent only to Railway GPS-capable clients and refreshed after valid network changes.
- Client-side destination selection, route following, basic track map matching, and a minecart-only navigation HUD.
- Automatic straight/left/right/turn-back instructions derived from recorded incoming and outgoing track geometry.
- Per-node custom transition instructions for physical controls whose meaning cannot be inferred from track geometry.
- Optional JourneyMap 6/API 2 integration for station and junction waypoints, recorded track overlays, and active-route highlighting.

Recording commands:

```mcfunction
/rgps record start <from> <to> <route_id>
/rgps record status
/rgps record finish
/rgps record cancel
/rgps route list
/rgps route info <route_id>
/rgps route remove <route_id>
/rgps route rename <old_id> <new_id>
/rgps instruction set <node_id> <incoming_route|*> <outgoing_route> <text...>
/rgps instruction list <node_id>
/rgps instruction remove <node_id> <incoming_route|*> <outgoing_route>
```

Recording must start within the source node's radius and finish within the destination node's radius. Stations currently use a 16-block radius; junctions use their configured `radius`. Finishing is always explicit. Recorded routes are directed by default.

Players with Railway GPS installed can begin the first navigation prototype near a registered node:

```mcfunction
/rgpsnav start <destination_id>
/rgpsnav status
/rgpsnav stop
```

The HUD compares the last part of the incoming recorded track with the first part of the outgoing track and displays straight, left, right, or turn-back. At the journey's starting node it compares the outgoing track with the minecart's movement or the player's view direction.

Use a custom transition instruction when the player must operate a named button or when geometry alone is ambiguous. For example:

```mcfunction
/rgps instruction set sigma_station incoming_line martyr_island Välj knappen för Martyrön
```

Use `*` as the incoming route for an instruction that applies when navigation starts at the node, and as a fallback for any arrival route:

```mcfunction
/rgps instruction set sigma_station * martyr_island Välj knappen för Martyrön
```

An exact `<incoming_route>><outgoing_route>` instruction takes precedence over the `*` fallback, and both take precedence over automatic geometry instructions.

## JourneyMap

JourneyMap is optional. When a compatible JourneyMap 6 build is installed on the client, Railway GPS automatically adds non-persistent station and junction waypoints and draws recorded track geometry. Routes remaining in the active navigation plan are highlighted in blue. No JourneyMap classes are bundled in the Railway GPS jar, and the integration entrypoint is not loaded when JourneyMap is absent.

## Requirements

- Java 25 or newer
- Minecraft Java Edition 26.3
- Fabric Loader 0.19.5 or newer
- Fabric API 0.165.0+26.3

## Build and test

```bash
./gradlew build
```

For a development client:

```bash
./gradlew runClient
```

Generated mod jars are written to `build/libs/`.

Every push to `main` is also built and tested by GitHub Actions. The distributable jar can be downloaded from that workflow run's **Artifacts** section as `railway-gps-<commit-sha>`.

## JSON data

Data is stored below the active world directory:

```text
railwaygps/
├── stations/
├── junctions/
└── routes/
```

IDs must match `[a-z0-9][a-z0-9_-]{0,63}`. A filename must match the object's `id`. Track positions are encoded as `[x, y, z]`, as described in the project specification.

Stations and junctions may contain an `instructions` object. Keys describe `incoming>outgoing` route transitions; `*>outgoing` is the start/fallback form:

```json
{
  "id": "sigma_station",
  "name": "Sigma station",
  "dimension": "minecraft:overworld",
  "position": [8, 81, 60],
  "instructions": {
    "incoming_line>martyr_island": "Välj knappen för Martyrön",
    "*>central": "Välj knappen mot Central"
  }
}
```

The implementation roadmap and product decisions remain in [Railway_GPS_specifikation.md](Railway_GPS_specifikation.md).
