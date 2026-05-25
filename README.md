# OriginSwitcher — Fabric Mod for Minecraft 1.20.1

A **client-side** Fabric mod that lets you change your Origins++ origin without the Origins mod being installed on the server.

---

## How It Works

Origins++ stores player origin data as a **component attached to the player entity**, synced by the server. This mod:

1. **Reads** origin data from the local component (always available client-side)
2. **Mutates** the local component — changing your displayed origin and its effects
3. **Optionally persists** the change via:
   - `/data merge` command (requires OP/operator permissions on the server)
   - A custom packet (if the server also has this mod installed — optional)

> **Important:** Client-side changes affect local power rendering and HUD. For a full persistent change (survives relog), you need OP or the server-side companion mod.

---

## Requirements

| Dependency | Version |
|---|---|
| Minecraft | 1.20.1 |
| Fabric Loader | ≥ 0.14.22 |
| Fabric API | 0.92.1+1.20.1 |
| Origins (Fabric) | 1.20.1-1.11.0+ |
| Origins++ / ExtraOrigins | Compatible version |

---

## Commands

All commands use the `/os` prefix.

| Command | Description |
|---|---|
| `/os setorigin <originId>` | Set your origin on the default layer (`origins:origin`) |
| `/os setorigin <layerId> <originId>` | Set your origin on a specific layer |
| `/os current` | Show your current origin on all layers |
| `/os list [query]` | Browse all registered origins (clickable!) |
| `/os layers` | List all available origin layers |
| `/os persist <layerId> <originId>` | Send `/data merge` to server for persistence (needs OP) |

### Example Usage

```
/os list                          — Browse all origins
/os list phantom                  — Search for "phantom"
/os setorigin origins:phantom     — Switch to Phantom on default layer
/os setorigin origins:origin origins:phantom  — Same, explicit layer
/os persist origins:origin origins:phantom    — Persist (needs OP)
```

---

## Building

```bash
# Clone or download this folder
cd origin-switcher

# Build the mod JAR
./gradlew build

# JAR will be in build/libs/originswitcher-1.0.0.jar
```

---

## Project Structure

```
src/main/java/com/originswitcher/
├── OriginSwitcherClient.java      — Mod entrypoint
├── command/
│   └── OriginCommand.java         — /os command with all subcommands
├── mixin/
│   └── PlayerEntityMixin.java     — Hook into player tick for deferred init
├── network/
│   └── OriginPacketHandler.java   — Custom packet channel (optional server mod)
└── util/
    └── OriginManager.java         — Core logic: get/set/list origins
```

---

## Persistence Modes

### Mode 1: Client-only (no server mod, no OP)
- Visual effects and local power state change
- Resets on relog

### Mode 2: OP + `/data` command
- Run `/os persist <layer> <origin>` after setting
- Requires operator permission on the server
- Persists across relogs

### Mode 3: Server-side companion (optional)
- Install this same mod JAR on the server too
- The custom `originswitcher:set_origin` packet channel activates
- Fully authoritative, no OP needed (configure permissions separately)

---

## Notes on Origins++ Compatibility

Origins++ (Extra Origins) registers its origins under its own namespace (e.g. `extraorigins:avian`). These are visible in `/os list` once the Origins layer data is synced to the client. The layer for the primary origin is typically `origins:origin`.

If you use multiple origin layers (e.g. from other addons), use `/os layers` to list them and `/os setorigin <layerId> <originId>` to target a specific one.
