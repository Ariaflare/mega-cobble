# Z-Moves — research & build reference

Research for the next Mega Cobble phase: **Z-Moves**. Data below is taken **from the bundled Pokémon
Showdown sim** that ships inside Cobblemon (`run/showdown/data/{items,moves}.js`,
`run/showdown/sim/battle-actions.js`) — i.e. the exact code that will run in battle — and
cross-checked against Serebii / Bulbapedia for the game-accurate rules.

Sources: [Serebii Z-Moves](https://www.serebii.net/sunmoon/zmoves.shtml),
[PokémonDB Z-Moves](https://pokemondb.net/sun-moon/zmoves),
[Bulbapedia Z-Move base power](https://www.pokemaniablog.com/2017/10/27/ZMoveBasePower.html).

---

## TL;DR — the architecture is the *same proven pattern as Mega Evolution*

**Cobblemon + its bundled Showdown already implement Z-Moves end to end.** We don't build the battle
mechanic; we plug items into it, exactly like we did for megas.

Already provided by Cobblemon/Showdown (free):
- **Battle sim** — `battle-actions.js` has `getZMove`, `getActiveZMove`, `canZMove`, the full
  base-power→Z-power conversion, status-Z bonus effects, and the "one Z per side per battle" rule
  (`side.zMoveUsed`).
- **All 35 Z-Crystals** already in `data/items.js`, **all 87 Z-Moves** already in `data/moves.js`.
  Unlike the Z-A megas (which we had to inject because the sim didn't know them), **Z-Moves are
  standard sim data — no injection needed.**
- **Client battle UI** — `ZPowerButton` / `BattleGimmickButton` (`Gimmick.Z_POWER`), the
  `ZPowerInstruction` interpreter, and a `ZMoveUsedEvent` / `CobblemonEvents.ZPOWER_USED` hook.
- **The gate** — `ShowdownActionRequest.sanitize()` shows the Z button **only if the player holds the
  `cobblemon:z_ring` key item**:
  ```
  MEGA_EVOLUTION  -> cobblemon:key_stone
  DYNAMAX         -> cobblemon:dynamax_band
  TERASTALLIZATION-> cobblemon:tera_orb
  Z_POWER         -> cobblemon:z_ring     <-- ours to satisfy
  ```

What **the mod must add** (each has a direct mega-side analogue that already works):
1. **Z-Crystal held items** — vanilla items tagged via `custom_data`, exposed to the sim so the held
   item reports the right Showdown id (e.g. `electriumz`). → mirror `MegaStoneHeldItemManager` /
   `MegaStones` (a `zcrystals.json` manifest + a `ZCrystalHeldItemManager`).
2. **A Z-Ring / Z-Power Ring item** bridged to the `cobblemon:z_ring` key item at battle start. →
   mirror `MegaItems.isKeyStone` + `MegaEvolution.syncKeyStone()` (add a `syncZRing()` that grants/
   removes `cobblemon:z_ring` based on whether the player carries a Z-Ring).
3. *(optional, later)* the flashy Z-Move **animation/pose** — Cobblemon plays a generic effect; a
   per-move cinematic is out of scope for v1.

**Net:** no sim injection, no form changes, no revert bookkeeping (a Z-Move is a one-shot attack, not
a persistent forme). This is **simpler than megas.**

---

## The 18 type crystals → generic Z-Move (`Z_MOVES` map in the sim)

Each type crystal turns a **damaging** move of its type into the type's Z-Move (power by the table
below), and a **status** move of its type into a Z-boosted version (bonus effect below).

| Type | Crystal (item id) | Z-Move |
|------|-------------------|--------|
| Normal | Normalium Z (`normaliumz`) | Breakneck Blitz |
| Fire | Firium Z (`firiumz`) | Inferno Overdrive |
| Water | Waterium Z (`wateriumz`) | Hydro Vortex |
| Electric | Electrium Z (`electriumz`) | Gigavolt Havoc |
| Grass | Grassium Z (`grassiumz`) | Bloom Doom |
| Ice | Icium Z (`iciumz`) | Subzero Slammer |
| Fighting | Fightinium Z (`fightiniumz`) | All-Out Pummeling |
| Poison | Poisonium Z (`poisoniumz`) | Acid Downpour |
| Ground | Groundium Z (`groundiumz`) | Tectonic Rage |
| Flying | Flyinium Z (`flyiniumz`) | Supersonic Skystrike |
| Psychic | Psychium Z (`psychiumz`) | Shattered Psyche |
| Bug | Buginium Z (`buginiumz`) | Savage Spin-Out |
| Rock | Rockium Z (`rockiumz`) | Continental Crush |
| Ghost | Ghostium Z (`ghostiumz`) | Never-Ending Nightmare |
| Dragon | Dragonium Z (`dragoniumz`) | Devastating Drake |
| Dark | Darkinium Z (`darkiniumz`) | Black Hole Eclipse |
| Steel | Steelium Z (`steeliumz`) | Corkscrew Crash |
| Fairy | Fairium Z (`fairiumz`) | Twinkle Tackle |

---

## The 17 signature / exclusive crystals (species-locked)

The sim enforces the species lock via `item.itemUser` in `getZMove`/`canZMove` — a signature crystal
only works for the listed species, and only converts the one specific base move (`zMoveFrom`).

| Crystal (id) | Z-Move | From base move | Species (`itemUser`) |
|--------------|--------|----------------|----------------------|
| Aloraichium Z (`aloraichiumz`) | Stoked Sparksurfer | Thunderbolt | Raichu-Alola |
| Decidium Z (`decidiumz`) | Sinister Arrow Raid | Spirit Shackle | Decidueye |
| Incinium Z (`inciniumz`) | Malicious Moonsault | Darkest Lariat | Incineroar |
| Primarium Z (`primariumz`) | Oceanic Operetta | Sparkling Aria | Primarina |
| Eevium Z (`eeviumz`) | Extreme Evoboost | Last Resort | Eevee |
| Snorlium Z (`snorliumz`) | Pulverizing Pancake | Giga Impact | Snorlax |
| Mewnium Z (`mewniumz`) | Genesis Supernova | Psychic | Mew |
| Pikanium Z (`pikaniumz`) | Catastropika | Volt Tackle | Pikachu |
| Pikashunium Z (`pikashuniumz`) | 10,000,000 Volt Thunderbolt | Thunderbolt | Pikachu (cap forms) |
| Tapunium Z (`tapuniumz`) | Guardian of Alola | Nature's Madness | Tapu Koko/Lele/Bulu/Fini |
| Marshadium Z (`marshadiumz`) | Soul-Stealing 7-Star Strike | Spectral Thief | Marshadow |
| Kommonium Z (`kommoniumz`) | Clangorous Soulblaze | Clanging Scales | Kommo-o |
| Lycanium Z (`lycaniumz`) | Splintered Stormshards | Stone Edge | Lycanroc (all forms) |
| Mimikium Z (`mimikiumz`) | Let's Snuggle Forever | Play Rough | Mimikyu |
| Solganium Z (`solganiumz`) | Searing Sunraze Smash | Sunsteel Strike | Solgaleo / Necrozma-Dusk-Mane |
| Lunalium Z (`lunaliumz`) | Menacing Moonraze Maelstrom | Moongeist Beam | Lunala / Necrozma-Dawn-Wings |
| Ultranecrozium Z (`ultranecroziumz`) | Light That Burns the Sky | Photon Geyser | Necrozma-Ultra |

**35 crystals total** (18 type + 17 signature).

Note `ultranecroziumz` doubles as the **Ultra Burst** trigger (a separate `Gimmick.ULTRA_BURST`); if
we ever want Ultra Necrozma, that's an extra hook — out of scope for a first Z pass.

---

## Base-power → Z-power conversion (damaging moves)

The standard in-game table (Showdown applies this automatically for moves without an explicit
override):

| Base power | Z-power |
|-----------:|--------:|
| 0–55 | 100 |
| 60 | 120 |
| 65–70 | 140 |
| 75–85 | 160 |
| 90–95 | 175 |
| 100 | 180 |
| 110–125 | 185 |
| 130 | 190 |
| 140–145 | 195 |
| 150+ | 200 |

**Explicit exceptions** the sim stores per-move in `moves.js` (`zMove.basePower`) — these override the
table. Confirmed present in the bundled data (distribution: 120×3, 140×11, 160×19, 170×1, 180×9,
185×2, 190×2, 220×1). Known ones: Mega Drain→120, Core Enforcer→140, Weather Ball/Hex→160, Flying
Press→170, Gear Grind→180, V-create→220, plus the 17 signature Z-moves' own fixed powers.

We do not implement any of this — the sim does. It's here so we can sanity-check damage in testing.

---

## Status-move Z bonus effects

A status move + its matching type crystal fires the **same status move** plus a one-time bonus. 72
status moves carry a `zMove` effect in the sim. Categories:

- `effect: clearnegativeboost` — reset the user's lowered stats (the most common; most setup/defensive
  status moves).
- `boost: {...}` — a flat stat boost on top (e.g. many +1 to a stat).
- `effect: heal` — full-ish HP heal (Aromatherapy, Belly Drum, Conversion 2, …).
- `effect: crit2` — raise crit ratio (Acupressure).
- `effect: healreplacement`, `redirect`, `curse`, `followme`, `clearnegativeboost` — a handful of
  move-specific ones.

Again — sim-driven. We just need the crystal held + the Z-Ring, and Showdown picks the right bonus.

---

## Battle mechanics (game-accurate, all enforced by the sim)

- **One Z-Move per battle, per side** (`side.zMoveUsed`). No revert needed — it's a single attack.
- **Trainer needs a Z-Ring/Z-Power Ring** to use any Z-Move → our `cobblemon:z_ring` bridge.
- **Pokémon must hold the matching Z-Crystal** for the move's type (or the exact signature crystal).
- Z-Moves **ignore the target's stat changes** when calculating damage, and **break through Protect**
  for chip damage (~25%). Status Z-Moves aren't blocked by the crystal-type mismatch. (All in-sim.)
- A Pokémon that's **transformed/mega/primal/ultra** can't also Z (`canZMove` early-out) — matches
  the "one gimmick" rule.

---

## Build plan (mirrors the Mega Evolution code, file-for-file)

| Mega side (exists) | Z-Move side (to add) |
|--------------------|----------------------|
| `MegaStones` + `megastones.json` | `ZCrystals` + `zcrystals.json` (35 entries: id, name, showdownId, type/species, `custom_data`) |
| `MegaStoneHeldItemManager` (exposes stones to the sim) | `ZCrystalHeldItemManager` (exposes crystals; report the Showdown crystal id for the held item) |
| `MegaItems.isKeyStone` + `syncKeyStone()` → `cobblemon:key_stone` | Z-Ring item + `syncZRing()` → `cobblemon:z_ring`, on `BATTLE_STARTED_PRE` |
| `/megacobble give <stone>` | extend `give` to Z-Crystals + the Z-Ring |
| `MegaShowdownInjector` (needed — sim lacked Z-A megas) | **not needed** — Z-Moves are already standard sim data |
| `applyMega`/`revert` (persistent forme) | **not needed** — Z-Move is a one-shot attack |

**Open questions to settle before coding:**
1. **Item art / IDs** — reuse the `custom_data` + `custom_model_data` scheme from Mega Stones (works
   for clients without the mod, resource-pack re-skinnable). One base item, 35 crystal variants + 1
   ring? Or lean on Cobblemon's own crystal items if it registers any (check first).
2. **Z-Ring naming** — "Z-Power Ring" (SM) vs "Z-Ring". Cobblemon's key item is `z_ring`.
3. **World / overworld Z?** Z-Moves are inherently in-battle only (they're attacks), so unlike megas
   there's no "world Z" analogue. Scope = in-battle.
4. **Signature crystals** — worth shipping all 17, or start with the 18 type crystals + a few marquee
   signatures (Pikanium, Snorlium, Mewnium)? All 35 are trivial data once the pipeline works.
