package com.aaroncraft.megacobble.zmove;

import com.aaroncraft.megacobble.item.MegaItems;
import com.aaroncraft.megacobble.item.ZCrystals;
import com.cobblemon.mod.common.api.battles.interpreter.BattleMessage;
import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.pokemon.helditem.HeldItemManager;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Exposes Mega Cobble's Z-Crystals to Cobblemon's bundled Pokémon Showdown sim.
 *
 * <p>Same idea as {@code MegaStoneHeldItemManager}: Cobblemon's built-in manager only maps
 * {@code cobblemon}-namespace items, so this reports the correct Showdown crystal id (e.g.
 * {@code "electriumz"}) when a Pokémon holds one of our {@code megacobble:} crystals. That's all the
 * sim needs to offer the Z-Move — it already knows every crystal and Z-Move, computes the power, and
 * enforces the per-type / signature-species restrictions itself.</p>
 *
 * <p>Held-item managers are consulted in priority order and the first non-null {@code showdownId}
 * wins ({@code HeldItemProvider.provideShowdownId}), so this coexists cleanly with the Mega Stone
 * manager: a Pokémon holds either a Mega Stone or a Z-Crystal, never both, and each manager returns
 * null for the other's items.</p>
 */
public class ZCrystalHeldItemManager implements HeldItemManager {

    @Override
    public String showdownId(BattlePokemon pokemon) {
        ItemStack held = pokemon.getEffectedPokemon().heldItem();
        ZCrystals.ZCrystal crystal = ZCrystals.byCustomData(held);
        // Signature crystals are species-locked, but the sim enforces that itself (via the item's
        // itemUser), so we expose the crystal unconditionally and let Showdown decide.
        return crystal != null ? crystal.showdownId() : null;
    }

    @Override
    public Component nameOf(String showdownId) {
        ZCrystals.ZCrystal crystal = ZCrystals.byShowdownId(showdownId);
        return Component.literal(crystal != null ? crystal.name() : showdownId);
    }

    @Override
    public void give(BattlePokemon pokemon, String showdownId) {
        ZCrystals.ZCrystal crystal = ZCrystals.byShowdownId(showdownId);
        if (crystal != null) {
            pokemon.getEffectedPokemon().swapHeldItem(MegaItems.createZCrystal(crystal), false, false);
        }
    }

    @Override
    public void take(BattlePokemon pokemon, String showdownId) {
        pokemon.getEffectedPokemon().removeHeldItem();
    }

    // Z-Crystals aren't started/ended by Showdown '-item' messages, and a Z-Crystal is NOT consumed
    // when its Z-Move is used (unlike a berry) — the trainer keeps it.
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
