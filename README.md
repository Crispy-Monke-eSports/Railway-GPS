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
- Navigation can start anywhere on a registered physical track; the occupied rail and its permitted direction
  become the first route leg, including on parallel double track.
- A minecart HUD with automatic straight/left/right/turn-back guidance and custom instructions.
- Optional JourneyMap overlays for control zones, physical tracks, names and the active route.
- A searchable player UI on `R` for choosing destinations, starting/stopping navigation and viewing status.
- A server-authorized **Tools** view for inspection, naming, surveys and administration.

There is one network implementation and one authoritative data file:

```text
<world>/railwaygps/network.json
```

The document currently uses schema version 1. This test-phase implementation intentionally does not
migrate data from earlier prototypes.

## Registering the railway

With LuckPerms or a compatible Fabric permissions provider, the four independent nodes are:

- `railwaygps.inspect` — read-only technical inspection and validation.
- `railwaygps.name` — track-label selection and publication only.
- `railwaygps.survey` — record and atomically publish validated minecart surveys.
- `railwaygps.admin` — physical topology, transitions, pair approval and destructive/publishing tools.

Provider grants are authoritative, including for operators. Without a provider, operators receive all four
tool capabilities and non-operators retain normal navigation only. Press `R` for navigation and choose
**Tools** to see only the capabilities the server currently grants. The `/rgps` commands are optional
alternatives governed by the same server checks.

### Control-point bounding boxes

Press `R` and choose **Tools** → **Administration**. Choose **Create station** or go to
**Redlight passages** → **Create redlight zone**. The world editor uses left and right clicks to
mark the two opposite block corners of an inclusive 3D zone; press `R` to return to the form,
then enter a name and save. No commands or manual IDs are needed.

The command interface remains available for administrators who prefer it:

```mcfunction
/rgps zone first
/rgps zone second
/rgps control create <id> <station|redlight|terminal|dimension_gate> <name...>
```

The zone determines when a vehicle has entered or left a station or junction. A one-block exit margin
prevents rapid enter/leave flicker at the boundary.

### Physical tracks and double track

Start inside any registered control-point zone while riding a minecart, drive through one or more other
control points, and finish inside the final zone:

```mcfunction
/rgps survey start
/rgps survey status
/rgps survey finish
/rgps survey cancel
```

Railway GPS detects the visited control points, splits the journey into physical tracks, assigns opaque
track IDs, and registers the exact transitions observed during the journey. Record each usable direction
separately. Rails between the same two control points automatically share a section but are never silently
paired. Railway GPS proposes pairs only when their recorded 3D geometry, spacing and alignment are
compatible. An administrator reviews the two highlighted rails under **Admin tools → Parallel-track
proposals** and explicitly approves or rejects each proposal. Commands remain an optional fallback:

```mcfunction
/rgps track pair <first_track> <opposite_track>
```

Approving reverse travel on the same physical rail is a separate admin action under **Verify travel on
existing track geometry**. It adds the independently verified direction to that track without cloning its
geometry or creating a parallel-track pair.

### Redlights, permitted transitions and instructions

**No chat commands are required for redlight administration.** Press `R` → **Tools** →
**Administration** → **Redlight passages**. From there:

1. Choose **Create redlight zone**, enter its name, then select two opposite corners in the
   world with left/right click. Press `R` to return and save. A redlight is a separate kind
   of control point, not a station.
2. Survey the connecting rails and verify the directions that are allowed to run through it.
3. Choose **Add redlight passage**. Select the redlight, a verified **incoming** direction,
   and a verified **outgoing** direction. Optionally enter a literal instruction for the driver,
   then create the passage. IDs are generated on the server; invalid/duplicate pairs are rejected.
4. In the paginated passage list, select any passage to independently enable/disable it or edit
   its instruction. The server publishes a new validated network revision after every change.

Navigation only crosses a redlight when that exact incoming-to-outgoing passage is registered
and enabled. Existing routes and instructions remain intact when a passage is disabled.

The command interface remains an optional admin alternative:

```mcfunction
/rgps transition add <id> <redlight> <incoming_track> forward <outgoing_track> forward
/rgps transition add mot_martyron sigma_redlight incoming_track forward martyr_track forward Välj knappen för Martyrön
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
