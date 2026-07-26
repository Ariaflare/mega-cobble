package com.aaroncraft.megacobble.battle;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.battles.BattleRegistry;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-battle damage readout ("damage tester"). Each opted-in player sees the exact damage every hit
 * deals — in their own battles — printed to chat. Opt in per player with {@code /megacobble damage on}
 * (available to everyone, not just ops).
 *
 * <p>Cobblemon exposes no per-hit damage event, and the HP write happens asynchronously on the
 * battle's dispatch queue (drained by {@code BattleRegistry.tick()}), so hooking the instruction sees
 * stale HP. Instead we poll each battling Pokémon's real server-side HP ({@link BattlePokemon#getHealth()})
 * once per server tick and report any drop — reading the settled sim value, not world-entity NBT
 * (which only syncs at battle end) or client HP ratios. Mirrors the tick-poll pattern already used for
 * {@code MegaEvolution.revertEndedBattleMegas()}.</p>
 */
public final class BattleDamageMonitor {

    /** Players who opted in to the readout (in-memory; resets on restart). */
    private static final Set<UUID> ENABLED = ConcurrentHashMap.newKeySet();
    /** BattlePokemon UUID -> HP as of the previous tick. Pruned to currently-battling Pokémon each tick. */
    private static final Map<UUID, Integer> LAST_HP = new HashMap<>();

    private BattleDamageMonitor() {
    }

    /** Enable/disable the readout for a player. @return the new state. */
    public static boolean setEnabled(UUID playerId, boolean enabled) {
        if (enabled) {
            ENABLED.add(playerId);
        } else {
            ENABLED.remove(playerId);
        }
        return enabled;
    }

    /** Flip the readout for a player. @return the new state. */
    public static boolean toggle(UUID playerId) {
        return setEnabled(playerId, !ENABLED.contains(playerId));
    }

    public static boolean isEnabled(UUID playerId) {
        return ENABLED.contains(playerId);
    }

    /** Called every server tick. Cheap no-op while nobody has opted in. */
    public static void tick(MinecraftServer server) {
        if (ENABLED.isEmpty()) {
            if (!LAST_HP.isEmpty()) {
                LAST_HP.clear();
            }
            return;
        }

        Set<UUID> seenBattles = new HashSet<>();
        Set<UUID> livePokemon = new HashSet<>();

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            PokemonBattle battle = BattleRegistry.INSTANCE.getBattleByParticipatingPlayer(player);
            // One battle can hold several players; only walk each battle once.
            if (battle == null || battle.getEnded() || !seenBattles.add(battle.getBattleId())) {
                continue;
            }
            // Only bother if at least one participant wants the readout.
            boolean anyViewer = false;
            for (ServerPlayer p : battle.getPlayers()) {
                if (ENABLED.contains(p.getUUID())) {
                    anyViewer = true;
                    break;
                }
            }

            for (BattleActor actor : battle.getActors()) {
                for (BattlePokemon battlePokemon : actor.getPokemonList()) {
                    if (battlePokemon.getGone()) {
                        continue;
                    }
                    UUID id = battlePokemon.getUuid();
                    livePokemon.add(id);
                    int current = battlePokemon.getHealth();
                    int max = Math.max(1, battlePokemon.getMaxHealth());
                    Integer previous = LAST_HP.put(id, current);
                    if (!anyViewer || previous == null || current >= previous) {
                        continue; // no viewer, new baseline, or a heal — only report damage
                    }
                    int damage = previous - current;
                    int percent = Math.round(damage * 100f / max);
                    Component line = Component.literal(
                            battlePokemon.getName().getString()
                                + " took " + damage + " (" + percent + "%)  [" + current + "/" + max + "]")
                        .withStyle(ChatFormatting.YELLOW);
                    for (ServerPlayer viewer : battle.getPlayers()) {
                        if (ENABLED.contains(viewer.getUUID())) {
                            viewer.sendSystemMessage(line);
                        }
                    }
                }
            }
        }

        // Drop entries for Pokémon no longer in an active battle, so the next battle re-baselines from
        // full HP instead of reporting a phantom heal/hit against a stale value.
        if (LAST_HP.size() != livePokemon.size()) {
            LAST_HP.keySet().retainAll(livePokemon);
        }
    }
}
