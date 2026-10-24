# Railway GPS

Railway GPS is a Fabric mod for non-invasive navigation on large vanilla Minecraft railway networks. Administrators register stations and junctions, record actual minecart paths, and clients use that server-maintained graph for route guidance.

This repository currently contains the first Phase 1 foundation:

- Fabric 26.3 project with separated common and client source sets.
- World-local `railwaygps/stations`, `junctions`, and `routes` JSON storage.
- Immutable station, junction, route, and network models.
- Whole-network validation and safe reload that retains the last valid in-memory network.
- Atomic creation of individual JSON files without accidental overwrite.
- Operator-only `/rgps station add <id>`, `/rgps node add <id>`, `/rgps network list`, and `/rgps reload` commands.

## Requirements

- Java 25 or newer
- Minecraft Java Edition 26.3
- Fabric Loader 0.20.1 or newer
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

## JSON data

Data is stored below the active world directory:

```text
railwaygps/
├── stations/
├── junctions/
└── routes/
```

IDs must match `[a-z0-9][a-z0-9_-]{0,63}`. A filename must match the object's `id`. Track positions are encoded as `[x, y, z]`, as described in the project specification.

The implementation roadmap and product decisions remain in [Railway_GPS_specifikation.md](Railway_GPS_specifikation.md).
