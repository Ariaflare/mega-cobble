package com.aaroncraft.megacobble.mega;

import com.aaroncraft.megacobble.item.MegaItems;
import com.aaroncraft.megacobble.item.MegaStones;
import com.cobblemon.mod.common.api.battles.interpreter.BattleMessage;
import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.pokemon.helditem.HeldItemManager;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Exposes Mega Cobble's Mega Stones to Cobblemon's bundled Pokémon Showdown sim.
 *
 * <p>Cobblemon's built-in held-item manager only maps items in the {@code cobblemon} namespace, so
 * a custom {@link HeldItemManager} is required for our {@code megacobble:} stones to be seen by the
 * battle simulator. With this registered (at a higher priority than Cobblemon's default), the sim
 * receives the correct Showdown stone id (e.g. "venusaurite") when a Pokémon holds one, which is
 * what lets Showdown enable its native Mega Evolution for the classic mega-capable species.</p>
 */
public class MegaStoneHeldItemManager implements HeldItemManager {

    @Override
    public String showdownId(BattlePokemon pokemon) {
        ItemStack held = pokemon.getEffectedPokemon().heldItem();
        MegaStones.MegaStone stone = MegaStones.byCustomData(held);
        if (stone == null) {
            return null;
        }
        // Form-restricted stones (e.g. Floettite -> only the Eternal Flower Floette) are only
        // exposed to the sim when the Pokémon has the required aspect, so other forms can't mega.
        if (stone.requiredAspect() != null
            && !pokemon.getEffectedPokemon().getAspects().contains(stone.requiredAspect())) {
            return null;
        }
        return stone.showdownId();
    }

    @Override
    public Component nameOf(String showdownId) {
        MegaStones.MegaStone stone = MegaStones.byShowdownId(showdownId);
        return Component.literal(stone != null ? stone.name() : showdownId);
    }

    @Override
    public void give(BattlePokemon pokemon, String showdownId) {
        MegaStones.MegaStone stone = MegaStones.byShowdownId(showdownId);
        if (stone != null) {
            pokemon.getEffectedPokemon().swapHeldItem(MegaItems.createStone(stone), false, false);
        }
    }

    @Override
    public void take(BattlePokemon pokemon, String showdownId) {
        pokemon.getEffectedPokemon().removeHeldItem();
    }

    // Mega Stones aren't triggered by Showdown '-item' start/end messages or consumed in battle.
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
