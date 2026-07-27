package com.aaroncraft.megacobble.mega;

import com.cobblemon.mod.common.api.battles.interpreter.BattleMessage;
import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.pokemon.helditem.HeldItemManager;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import net.minecraft.network.chat.Component;

/**
 * Exposes a VIRTUAL "Rayquazite" to the bundled Showdown sim for a Dragon-Ascent Rayquaza.
 *
 * <p>Mega Rayquaza is stoneless — it Mega Evolves from knowing Dragon Ascent, not a Mega Stone. The
 * sim supports that (its move-based {@code canMegaEvo} branch), but at Gen 9 that branch only fires
 * when the battle ruleset carries the "past" tag, which does not reliably reach Cobblemon's in-process
 * sim. The sim's ITEM-based {@code canMegaEvo} branch, however, has no such gate. So we tell the sim a
 * Dragon-Ascent Rayquaza "holds" a Rayquazite (a mega stone injected via {@code custom_mega_showdown.json}),
 * which makes Showdown offer Mega Rayquaza.</p>
 *
 * <p>This is entirely server-side and invisible to the player: it never reads or changes the Pokémon's
 * real held item, and no Rayquazite item exists in the game — the id is only ever handed to the sim.</p>
 */
public class RayquazaMegaHeldItemManager implements HeldItemManager {

    @Override
    public String showdownId(BattlePokemon pokemon) {
        return MegaEvolution.stonelessMegaSimItem(pokemon.getEffectedPokemon());
    }

    @Override
    public Component nameOf(String showdownId) {
        return Component.literal("rayquazite".equals(showdownId) ? "Rayquazite" : showdownId);
    }

    // Purely virtual: never give or take a real held item on the Cobblemon side.
    @Override
    public void give(BattlePokemon pokemon, String showdownId) {
    }

    @Override
    public void take(BattlePokemon pokemon, String showdownId) {
    }

    @Override
    public void handleStartInstruction(BattlePokemon pokemon, PokemonBattle battle, BattleMessage battleMessage) {
    }

    @Override
    public void handleEndInstruction(BattlePokemon pokemon, PokemonBattle battle, BattleMessage battleMessage) {
    }

    @Override
    public boolean shouldConsumeItem(BattlePokemon pokemon, PokemonBattle battle, String showdownId) {
        return false;
    }
}
