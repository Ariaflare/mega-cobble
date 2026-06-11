# Mega Cobble — Command Reference

Every command lives under **`/megacobble`**.

**Conventions**

- **Target** — commands that act on a Pokémon default to the **one you're looking at** (must be yours,
  within ~6 blocks and sent out). Add **`slot <1-6>`** to target a party slot instead.
- **Permissions** — `worldmega` is available to **all players** (it's gated by the Key Stone / Mega
  Stone instead). `give`, `variant`, `skin`, and `config` require **op (permission level 2)**.
- Notation: `<required>`, `[optional]`, `a|b` = choose one.

## Quick reference

| Command | Perm | What it does |
| --- | --- | --- |
| `/megacobble worldmega [on\|off\|toggle] [slot <1-6>]` | all | Mega-evolve a Pokémon out of battle |
| `/megacobble give <stone\|random> [count] [<targets>]` | op | Give a Mega Stone / Key Stone / random stone to you or player(s) |
| `/megacobble variant list` | op | List the named-look catalog |
| `/megacobble variant apply\|remove <variant> [slot <1-6>]` | op | Apply / remove a named look |
| `/megacobble variant reload` | op | Re-read the look catalog from disk |
| `/megacobble skin set <aspect> [slot <1-6>]` | op | Force any skin aspect on one Pokémon |
| `/megacobble skin set <aspect> all` | op | **Globally** skin every Pokémon of the looked-at species |
| `/megacobble skin clear [slot <1-6>]` | op | Reset one Pokémon to default / a datapack |
| `/megacobble skin clear all` | op | Remove the global skin for the looked-at species |
| `/megacobble config` | op | Show current config |
| `/megacobble config <key> <true\|false>` | op | Set a config value |

---

## `worldmega` — out-of-battle Mega Evolution

```
/megacobble worldmega [on|off|toggle] [slot <1-6>]
```

Mega-evolves the targeted Pokémon **in the overworld** (for exploration, riding, screenshots) — the
command equivalent of the interaction-wheel "Mega Evolve" button. No argument = **toggle**.

- **Permission:** all players.
- **Gate (default):** you must carry a **Key Stone** and the Pokémon must hold its **Mega Stone**
  (configurable — see `config`). It reads the held stone, so Charizard/Mewtwo get the right X/Y form.
- **Reverts automatically when a battle starts**, so the real in-battle Showdown mega takes over.
- Rideability carries through the mega form for normally-rideable species.

**Examples**
```
/megacobble worldmega                 # toggle the Pokémon you're looking at
/megacobble worldmega off             # revert it
/megacobble worldmega on slot 1       # mega the first Pokémon in your party
```

## `give` — get stones

```
/megacobble give <stone> [count] [<targets>]
/megacobble give random  [count] [<targets>]
```

Gives a **Mega Stone** or the **Key Stone**. `<stone>` **tab-completes** `random`, `key_stone`, and
every Mega Stone id. `[count]` is 1–64 (default 1). `[<targets>]` is a **player selector** (`@p`, `@a`,
`@r`, a username, …) — **omit it to give to yourself**. `random` gives each recipient an independently
**random Mega Stone**.

- **Permission:** op (for everything, including giving to other players).

**Examples**
```
/megacobble give key_stone                 # Key Stone to yourself
/megacobble give venusaurite 5             # 5 Venusaurite to yourself
/megacobble give charizardite_x Steve      # to player Steve
/megacobble give random                    # a random Mega Stone to yourself
/megacobble give random 1 @a               # a random stone to every online player
```

## `variant` — named looks

```
/megacobble variant list
/megacobble variant apply|remove <variant> [slot <1-6>]
/megacobble variant reload
```

Applies a **named look** from the catalog (each look is one or more aspects that resolvers map to a
model/texture/animation). `<variant>` **tab-completes** from the catalog. `apply` adds the look's
aspects; `remove` takes them away.

- **Permission:** op.
- **Catalog:** the bundled `variants.json` plus an optional editable overlay at
  `config/megacobble/variants.json` (same-id entries override). Built-in: `mega`, `mega_x`, `mega_y`.
- `reload` re-reads the catalog after you edit the overlay — no restart needed.
- Scaffold new looks with `tools/gen_look.py` (point at an existing Cobblemon model or custom assets).

**Examples**
```
/megacobble variant list
/megacobble variant apply mega
/megacobble variant remove mega slot 2
/megacobble variant reload
```

## `skin` — force any aspect (incl. datapack skins)

```
/megacobble skin set <aspect> [slot <1-6>]      # one Pokémon (looked-at, or a party slot)
/megacobble skin set <aspect> all               # GLOBAL: every Pokémon of the looked-at species
/megacobble skin clear [slot <1-6>]             # reset one Pokémon
/megacobble skin clear all                      # remove the global skin for the looked-at species
```

The mod is **skin-agnostic** — it only sets aspects, and a resolver (the substitute doll by default,
or any installed datapack/resource-pack resolver with `order > 5`) decides the look. `skin set` forces
**any** aspect, so you can apply skins an external datapack defines.

- **Per-Pokémon** (`[slot]` or looked-at): forces the aspect on that one Pokémon; `skin clear` resets it.
- **Global** (`all`): look at any Pokémon of a species, and the skin is applied to **every** Pokémon of
  that species — loaded, boxed, or caught later — via a server-side aspect provider (synced to clients,
  persisted to `config/megacobble/global_skins.json`). `skin clear all` removes it. Already-loaded
  Pokémon are refreshed immediately; the count is reported.

- **Permission:** op.
- For Cobblemon *feature*-based datapack skins, Cobblemon's own `/pokeedit <feature>=<value>` is the
  native per-Pokémon setter; `skin set` is the lower-level "force this aspect" path that works for any
  aspect, and `skin set … all` is the species-wide version.

**Examples**
```
/megacobble skin set blastoise_skin           # one Pokémon you're looking at
/megacobble skin set blastoise_skin all        # ALL Venusaurs (if looking at a Venusaur)
/megacobble skin clear all                     # remove that global skin
```

## `config` — world-mega settings

```
/megacobble config
/megacobble config <key> <true|false>
```

Shows or edits the config (persisted to `config/megacobble.json`). `<key>` **tab-completes**.

- **Permission:** op.

| Key | Default | Effect |
| --- | --- | --- |
| `worldMegaEnabled` | `true` | Master switch for out-of-battle mega (wheel + command). |
| `requireKeyStone` | `true` | Require a Key Stone in the inventory to world-mega. |
| `requireMegaStone` | `true` | Require the Pokémon to hold its Mega Stone to world-mega. |
| `allowRideInMega` | `true` | When `false`, hides the "Mega Evolve" option from the interaction wheel. |
| `revertOnBattleStart` | `true` | Revert world megas when a battle starts. |

**Examples**
```
/megacobble config
/megacobble config requireKeyStone false
```
