# Mega Cobble

A Fabric add-on that brings **Mega Evolution** to
[Cobblemon](https://cobblemon.com/).

> Status: working for the classic roster. All 47 Gen 6 / ORAS Mega Stones are in,
> and Mega Evolution runs through Cobblemon's bundled Pokémon Showdown sim, so it's
> a **real in-battle transformation** (correct stats, typing, and ability) — not a
> cosmetic-only change. The Legends Z-A megas are researched and their data is
> stored (`za_megas.json`), but not yet wired in-battle.

## Features

- **All 47 classic Mega Stones** (every Gen 6 / ORAS mega, including Charizard &
  Mewtwo X/Y) plus a **Key Stone**, grouped in a dedicated **Mega Stones** creative tab.
- **Native, in-battle Mega Evolution** — give a Pokémon its matching Mega Stone as a
  held item and carry a Key Stone; Cobblemon's own mega button appears in the Fight
  menu, and choosing a move + mega runs the real Showdown mega (stats / typing / ability).
- The mega'd Pokémon is renamed `Mega-<Species>` (e.g. `Mega-Charizard-X`) and
  **reverts** automatically at battle end, flee, or faint.
- Visuals use the **substitute doll** as a placeholder (no per-mega 3D models yet).

## How it works

Mega is driven entirely through Cobblemon's existing systems — no edits to Cobblemon's
code:

- A custom `HeldItemManager` exposes our `megacobble:` Mega Stones to the Showdown sim
  (Cobblemon's default only handles its own namespace), so the sim sets `canMegaEvo`.
- At battle start, a bridge grants the `cobblemon:key_stone` key-item while the player
  is carrying a Key Stone, so Cobblemon's native gate (`sanitize()`) lets the mega
  button through.
- A `MEGA_EVOLUTION` event hook mirrors the form change on the Minecraft side (model /
  name); the Showdown sim handles the actual battle mechanics.

## Using it in-game

1. Get the items from the **Mega Stones** creative tab, or e.g.
   `/give @s megacobble:charizardite_x` and `/give @s megacobble:key_stone`.
2. Give a Pokémon its matching Mega Stone in its **held-item** slot.
3. Keep a **Key Stone** in your inventory and start a battle.
4. On the Fight screen, use the mega button, pick a move, and it Mega Evolves.

## Target stack

| Component      | Version           |
| -------------- | ----------------- |
| Minecraft      | 1.21.1            |
| Mod loader     | Fabric            |
| Fabric Loader  | >= 0.17.2         |
| Fabric API     | >= 0.116.6+1.21.1 |
| Cobblemon      | 1.7.3+1.21.1      |
| Java           | 21                |

## Project layout

```
src/main/java/com/aaroncraft/megacobble/
  MegaCobble.java                 common entrypoint: registration + battle event hooks
  client/MegaCobbleClient.java    client entrypoint
  item/ModItems.java              Key Stone + the Mega Stones creative tab
  item/MegaStones.java            loads megastones.json, registers a stone item per entry
  mega/MegaEvolution.java         key-item bridge, in-battle form mirror, revert
  mega/MegaStoneHeldItemManager.java   exposes our stones to the Showdown sim
src/main/resources/
  megastones.json                 manifest: stone -> species / form / aspect
  za_megas.json                   reference data for Legends Z-A megas (future work)
  assets/megacobble/...           item models, textures, lang
  assets/cobblemon/bedrock/pokemon/resolvers/megacobble/   substitute-doll resolvers
tools/gen_megastones.py           generates textures, models, resolvers, lang, manifest
```

## Building

You need JDK 21 on your PATH. From the project root:

```bash
./gradlew build          # Linux / macOS
.\gradlew.bat build      # Windows
```

The built mod jar lands in `build/libs/`.

## Running in dev

```bash
./gradlew runClient      # launch a dev client with the mod + Cobblemon loaded
./gradlew runServer      # launch a dev server
```

## Build notes

- Uses **official Mojang mappings** to line up 1:1 with the mappings Cobblemon is
  developed against.
- Uses **Fabric Loom 1.11 + Gradle 8.14** and applies the **Kotlin plugin** — not for
  Kotlin sources (there are none) but so Loom remaps Cobblemon's Kotlin `@Metadata` for
  the dev runtime. Without it, Cobblemon's reflection looks up intermediary names that
  don't exist at runtime and crashes on boot.

## Dependencies & the Cobblemon jar

Cobblemon is pulled automatically from its
[Maven repository](https://maven.impactdev.net/) at build time — you do **not** need a
local jar to build. The provided `Cobblemon-fabric-1.7.3+1.21.1.jar` (~135 MB) is
**git-ignored** and kept only as a local reference.

## Limitations / roadmap

- Mega forms render as the **substitute doll** placeholder; real per-mega models are a
  future art task.
- **Legends Z-A megas** aren't playable in-battle yet — Cobblemon's bundled Showdown has
  no data for them. Their types/abilities/stats are stored in `za_megas.json` for a
  planned implementation (which also needs new Showdown data + four new abilities).

## License

All rights reserved (private).
