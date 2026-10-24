# Railway GPS

Railway GPS is a Fabric mod for navigation on large vanilla Minecraft railway networks. The authoritative
model describes physical rails, multi-track sections, control-point zones, allowed transitions and optional
names such as Centralbron or Riksettan.

The mod currently provides:

- Compound 3D zones for stations, junctions, terminals and dimension gates.
- Directed physical-track recording from a minecart, including position-jump protection.
- Separate parallel rails grouped into one section and optionally paired as opposite directions.
- Explicit permitted transitions through control points, with optional custom instructions such as
  “Välj knappen för Martyrön”.
- Partial and multi-track names selected between two points without driving the section again.
- Server-authoritative validation, atomic JSON storage and digest-verified client synchronization.
- Shortest-path navigation over verified tracks and permitted transitions.
- A minecart HUD with automatic straight/left/right/turn-back guidance and custom instructions.
- Optional JourneyMap overlays for control zones, physical tracks, names and the active route.

There is one network implementation and one authoritative data file:

```text
<world>/railwaygps/network.json
```

The document currently uses schema version 1. This test-phase implementation intentionally does not
migrate data from earlier prototypes.

## Registering the railway

All editing commands require operator permissions.

### Control-point bounding boxes

Stand at two opposite block corners of the inclusive 3D zone, then create the control point:

```mcfunction
/rgps zone first
/rgps zone second
/rgps control create <id> <station|junction|terminal|dimension_gate> <name...>
```

The zone determines when a vehicle has entered or left a station or junction. A one-block exit margin
prevents rapid enter/leave flicker at the boundary.

### Physical tracks and double track

Start inside the source zone while riding a minecart, drive to the destination zone, and finish explicitly:

```mcfunction
/rgps survey start <from_control_point> <to_control_point> <track_id>
/rgps survey status
/rgps survey finish
/rgps survey cancel
```

Record each usable direction separately. Rails between the same two control points automatically share a
section. Pair opposite directional rails when they represent the two sides of the same railway:

```mcfunction
/rgps track pair <first_track> <opposite_track>
```

### Permitted transitions and instructions

Navigation only crosses a control point when the exact incoming-to-outgoing movement is registered:

```mcfunction
/rgps transition add <id> <control_point> <incoming_track> forward <outgoing_track> forward
```

Append text when the player must operate a named control:

```mcfunction
/rgps transition add mot_martyron sigma_station incoming_track forward martyr_track forward Välj knappen för Martyrön
```

### Naming part of the railway

Mark two positions near already recorded rails:

```mcfunction
/rgps label point start
/rgps label point end
/rgps label candidates
/rgps label create <id> <way|bridge|tunnel|district|custom> <candidate> <include_paired> <name...>
/rgps label cancel
```

Every valid branch is presented as a candidate; an ambiguous path is never selected silently. Set
`include_paired` to `true` to apply the name to the corresponding opposite rail as well. A name can span
part of one track or many tracks and transitions, so long roads such as Riksettan can pass several signals.

Inspect counts or reload the validated document with:

```mcfunction
/rgps network list
/rgps reload
```

## Navigation

Start near or inside a registered control-point zone:

```mcfunction
/rgpsnav start <destination_id>
/rgpsnav status
/rgpsnav stop
```

The planner follows track direction and only uses enabled explicit transitions. JourneyMap highlights the
remaining physical tracks in the active plan.

## Client inspector

The built-in inspector works without JourneyMap. It draws nearby control-point bounding boxes and physical
track geometry directly in the world; the active route is emphasized. Rendering is limited to 256 blocks
from the camera.

```mcfunction
/rgpsinspect toggle
/rgpsinspect status
/rgpsinspect list controls
/rgpsinspect list tracks
/rgpsinspect list labels
```

JourneyMap remains optional and provides the corresponding overview on its minimap and fullscreen map.

## Requirements

- Java 25 or newer
- Minecraft Java Edition 26.3
- Fabric Loader 0.19.5 or newer
- Fabric API 0.165.0+26.3
- JourneyMap 6 with API 2 is optional

## Build and test

```bash
./gradlew build
```

For a development client:

```bash
./gradlew runClient
```

Generated jars are written to `build/libs/`. Every push to `main` is built and tested by GitHub Actions;
the distributable jar is available from that run's Artifacts section as `railway-gps-<commit-sha>`.

The complete product and data-model decisions are documented in
[Railway_GPS_specifikation.md](Railway_GPS_specifikation.md).
