# Mega Cobble

A Fabric add-on that brings **Mega Evolution** to
[Cobblemon](https://cobblemon.com/) — including a data-driven pipeline for adding
**custom megas**.

> Status: working in-battle. All **47 classic Gen 6 / ORAS** megas plus **18 Legends Z-A**
> megas run through Cobblemon's bundled Pokémon Showdown sim, so they're **real in-battle
> transformations** (correct stats, typing, and ability), not cosmetic-only. The same
> pipeline can add fully custom megas (see *Adding a custom Mega* below).

## Features

- **47 classic Mega Stones** (every Gen 6 / ORAS mega, incl. Charizard & Mewtwo X/Y) and
  **18 Legends Z-A** megas, plus a **Key Stone**, grouped in a dedicated **Mega Stones**
  creative tab.
- **Native, in-battle Mega Evolution** — give a Pokémon its matching Mega Stone as a held
  item and carry a Key Stone; Cobblemon's own mega button appears in the Fight menu, and
  choosing a move + mega runs the real Showdown mega (stats / typing / ability).
- The mega'd Pokémon is renamed `Mega-<Species>` (e.g. `Mega-Charizard-X`) and **reverts**
  automatically at battle end, flee, or faint.
- A **data-driven custom-mega system**: classic megas reuse Cobblemon's bundled sim data;
  Z-A / custom megas add their form, stone, and (if needed) a brand-new ability through a
  generator — no Java changes required.
- Visuals use the **substitute doll** as a placeholder (no per-mega 3D models yet).

## How it works

Everything runs through Cobblemon's existing systems — no edits to Cobblemon's code:

- A custom `HeldItemManager` exposes our `megacobble:` Mega Stones to the Showdown sim
  (Cobblemon's default only handles its own namespace), so the sim sets `canMegaEvo`.
- At battle start, a bridge grants the `cobblemon:key_stone` key-item while the player
  carries a Key Stone, so Cobblemon's native gate (`sanitize()`) lets the mega button
  through.
- A `MEGA_EVOLUTION` event hook mirrors the form change on the Minecraft side (model /
  name); the Showdown sim handles the actual battle mechanics; revert is hooked to battle
  end / flee / faint.

For megas **not** in Cobblemon's bundled sim (Z-A and custom), three extra data sources fill
the gaps:

- `data/cobblemon/species_additions/<species>_mega.json` — adds the mega **form** (stats /
  types / ability / aspect / `requiredItem`); Cobblemon auto-syncs it to the sim.
- `data/megacobble/abilities/<id>.js` — any **new ability** as a Cobblemon datapack ability
  (registers it on the Cobblemon side *and* forwards it to the sim).
- `custom_mega_showdown.json` — the **mega-stone item** def (`megaStone`/`megaEvolves`),
  injected into the sim at battle start by `MegaShowdownInjector` (Cobblemon won't send our
  items).

## Using it in-game

1. Get the items from the **Mega Stones** creative tab, or e.g.
   `/give @s megacobble:charizardite_x` and `/give @s megacobble:key_stone`.
2. Give a Pokémon its matching Mega Stone in its **held-item** slot.
3. Keep a **Key Stone** in your inventory and start a battle.
4. On the Fight screen, use the mega button, pick a move, and it Mega Evolves.

## Adding a custom Mega

The pipeline is data-driven; adding a mega (Z-A or fully custom) means editing a couple of
data files and running the generators — no code changes.

1. **Define the mega's data.** Add an entry to `src/main/resources/za_megas.json`:
   ```json
   {
     "species": "lucario", "form": "Mega", "aspect": "mega",
     "type1": "fighting", "type2": "steel", "ability": "adaptability",
     "baseStats": { "hp": 70, "attack": 145, "defence": 88,
                    "special_attack": 140, "special_defence": 70, "speed": 112 }
   }
   ```
   - `species` is the lowercase Cobblemon species id (it must already exist in Cobblemon — a
     brand-new Pokémon would have to be added first).
   - `ability` is a Showdown ability id. Use an existing one (`adaptability`, `protean`, …)
     or a brand-new one you define in step 3.

2. **Give it a Mega Stone.** In `tools/gen_custom_megas.py`, add the species to `STONE_NAMES`
   with the stone's display name:
   ```python
   'lucario': 'Lucaronite',
   ```

3. **(Only if the ability is brand-new)** In `tools/gen_custom_megas.py` add it to:
   - `NEW_ABILITY_ID` — map the `za_megas.json` ability value to its Showdown id,
   - `NEW_ABILITY_JS` — the `(display name, JS effect)` (the sim `eval`s the JS object;
     use Showdown's ability API: `onModifyType`, `onBasePower`, `onDamagingHit`, …),
   - `NEW_ABILITY_DESC` — the summary-screen description.

4. **Generate.** Run both generators (classic first, then the custom batch which appends):
   ```bash
   python tools/gen_megastones.py
   python tools/gen_custom_megas.py
   ```
   This writes the species-addition form, the stone (manifest entry + texture + model +
   lang), the substitute resolver, the sim item injection, and any new ability file + lang.

5. **Build & run.** `./gradlew runClient`. The held-item manager, key-item bridge, sim
   injector, and form mirror all pick it up automatically — give the Pokémon its stone,
   carry a Key Stone, and battle.

> Special forms (e.g. **Mega Floette** only applying to AZ's Eternal-Flower Floette) need
> form-specific handling and aren't covered by the simple flow above.

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
  mega/MegaShowdownInjector.java  injects custom mega-stone item defs into the sim
src/main/resources/
  megastones.json                 manifest: stone -> species / form / aspect
  za_megas.json                   Legends Z-A / custom mega data (types, abilities, stats)
  custom_mega_showdown.json       mega-stone item defs injected into the sim
  data/cobblemon/species_additions/   the mega forms (Z-A / custom)
  data/megacobble/abilities/      new abilities as Cobblemon datapack ability JS
  assets/megacobble/...           item models, textures, lang
  assets/cobblemon/bedrock/pokemon/resolvers/megacobble/   substitute-doll resolvers
tools/gen_megastones.py           classic stones: textures, models, resolvers, lang, manifest
tools/gen_custom_megas.py         Z-A / custom megas: species_additions, abilities, sim defs, assets
```

## Building & running

JDK 21 on your PATH, then:

```bash
.\gradlew.bat build        # build the jar (lands in build/libs/)
.\gradlew.bat runClient    # dev client with the mod + Cobblemon
```

## Build notes

- Uses **official Mojang mappings** to line up 1:1 with the mappings Cobblemon is developed
  against.
- Uses **Fabric Loom 1.11 + Gradle 8.14** and applies the **Kotlin plugin** — not for Kotlin
  sources (there are none) but so Loom remaps Cobblemon's Kotlin `@Metadata` for the dev
  runtime. Without it, Cobblemon's reflection looks up intermediary names that don't exist
  at runtime and crashes on boot.

## Dependencies & the Cobblemon jar

Cobblemon is pulled automatically from its
[Maven repository](https://maven.impactdev.net/) at build time — you do **not** need a local
jar to build. The provided `Cobblemon-fabric-1.7.3+1.21.1.jar` (~135 MB) is **git-ignored**
and kept only as a local reference.

## Limitations / roadmap

- Mega forms render as the **substitute doll** placeholder; real per-mega models are a
  future art task.
- **Mega Sol** (Meganium) is a registered stub — the mega works, but the ability has no
  effect yet (a faithful weather-treatment implementation is TODO).
- **Mega Floette** is deferred — it should only apply to AZ's Eternal-Flower Floette
  (`flower-eternal` form), which needs special handling.
- A handful of Z-A megas are not yet included because their data (ability or stats) is
  incomplete in `za_megas.json`.

## Changelog

Newest first.

- **Shiny support** — mega forms (and the Eternal Floette base) render the **shiny**
  substitute doll for shiny Pokémon, the normal doll otherwise.
- **18 Legends Z-A megas** via a data-driven custom-mega pipeline (`species_additions` +
  Cobblemon datapack abilities + a battle-start sim injector). Adds the new abilities
  Dragonize, Piercing Drill, and Mega Sol (stub). Introduces per-stone `requiredAspect`,
  used to gate **Floettite** to AZ's Eternal-Flower Floette.
- **Native Showdown mega** — Mega Evolution now runs through Cobblemon's bundled Pokémon
  Showdown sim, so the classic megas get **real in-battle stats / typing / ability**. Uses a
  custom `HeldItemManager` (exposes our stones to the sim), a Key Stone → key-item bridge,
  and a `MEGA_EVOLUTION` hook for the visual. Replaced the earlier custom button + packet.
- **All 47 classic Mega Stones** (Gen 6 / ORAS) in a dedicated **Mega Stones** creative tab.
- **Mega Evolution proof of concept (Venusaur)** — Key Stone + Venusaurite, an in-battle
  Mega button, the form transform, the substitute-doll placeholder model, name change, and
  revert on battle end / flee / faint.
- **Initial scaffold** — Fabric Loom project targeting Minecraft 1.21.1, Cobblemon 1.7.3,
  Java 21.

## License

All rights reserved (private).
