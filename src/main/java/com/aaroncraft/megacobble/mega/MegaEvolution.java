package com.aaroncraft.megacobble.mega;

import com.aaroncraft.megacobble.MegaCobble;
import com.aaroncraft.megacobble.item.MegaStones;
import com.aaroncraft.megacobble.item.ModItems;
import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.storage.party.PartyStore;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.pokemon.FormData;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.util.PlayerExtensionsKt;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-authoritative Mega Evolution. Looks up the Pokémon in the player's party,
 * validates the Mega Stone (held) and Key Stone (inventory), then swaps the Pokémon to its
 * mega form. Setting {@code form} fires Cobblemon's FormUpdatePacket, so the change syncs
 * to all clients automatically.
 */
public final class MegaEvolution {

    private MegaEvolution() {}

    /** Pre-mega state kept so a Pokémon can be reverted at battle end / faint / flee. */
    private static final Map<UUID, Original> MEGA_STATES = new ConcurrentHashMap<>();

    private record Original(Set<String> forcedAspects, MutableComponent nickname) {}

    public static void evolve(ServerPlayer player, UUID pokemonUuid) {
        MegaCobble.LOGGER.info("[Mega Cobble] Received mega-evolve request from {} for pokemon {}.",
            player.getGameProfile().getName(), pokemonUuid);

        // 1) Player must carry a Key Stone in their inventory.
        boolean hasKeyStone = player.getInventory().hasAnyMatching(stack -> stack.is(ModItems.KEY_STONE));
        if (!hasKeyStone) {
            MegaCobble.LOGGER.info("[Mega Cobble] Rejected: no Key Stone in inventory.");
            return;
        }

        // 2) Find the requested Pokémon in the player's party.
        PartyStore party = PlayerExtensionsKt.party(player);
        Pokemon target = null;
        for (Pokemon pokemon : party) {
            if (pokemon != null && pokemon.getUuid().equals(pokemonUuid)) {
                target = pokemon;
                break;
            }
        }
        if (target == null) {
            MegaCobble.LOGGER.info("[Mega Cobble] Rejected: pokemon {} not found in party.", pokemonUuid);
            return;
        }

        // 3) The Pokémon must be holding a Mega Stone that matches its species.
        ItemStack held = target.heldItem();
        MegaStones.MegaStone stone = MegaStones.byItem(held.getItem());
        if (stone == null) {
            MegaCobble.LOGGER.info("[Mega Cobble] Rejected: {} is not holding a Mega Stone (held = {}).",
                target.getSpecies().getName(), held.isEmpty() ? "nothing" : held.getItem());
            return;
        }
        if (!stone.species().equalsIgnoreCase(target.getSpecies().getName())) {
            MegaCobble.LOGGER.info("[Mega Cobble] Rejected: {} cannot use {} (it belongs to {}).",
                target.getSpecies().getName(), stone.name(), stone.species());
            return;
        }

        // 4) Resolve the specific mega form this stone unlocks (e.g. "Mega", "Mega-X").
        FormData megaForm = findFormByName(target, stone.form());
        if (megaForm == null) {
            MegaCobble.LOGGER.info("[Mega Cobble] Rejected: form '{}' not found for species {}.",
                stone.form(), target.getSpecies().getName());
            return;
        }

        // Remember the pre-mega state so we can revert it when the battle ends.
        MEGA_STATES.put(target.getUuid(),
            new Original(new HashSet<>(target.getForcedAspects()), target.getNickname()));

        // 5) Apply the transformation.
        //    Forcing the form's aspect (e.g. "mega") is what actually changes the rendered model:
        //    Cobblemon resolves the model from the Pokémon's aspects, and forcedAspects' setter
        //    recalculates aspects, updates the form, and syncs to the client automatically.
        String oldForm = target.getForm().getName();
        Set<String> forced = new HashSet<>(target.getForcedAspects());
        forced.addAll(megaForm.getAspects());
        target.setForcedAspects(forced);
        target.setForm(megaForm);
        target.setNickname(Component.literal(buildMegaName(target, megaForm)));

        MegaCobble.LOGGER.info("[Mega Cobble] SUCCESS: {} ({}) mega evolved {} -> {} (nickname '{}').",
            player.getGameProfile().getName(), target.getUuid(), oldForm, megaForm.getName(),
            buildMegaName(target, megaForm));
    }

    /**
     * Reverts a single Pokémon back to its pre-mega state, if it was mega evolved. Restoring the
     * original forced aspects (dropping "mega") makes Cobblemon re-select the standard form, which
     * syncs to the client automatically.
     */
    public static void revert(Pokemon pokemon) {
        Original original = MEGA_STATES.remove(pokemon.getUuid());
        if (original == null) {
            return;
        }
        pokemon.setForcedAspects(original.forcedAspects());
        pokemon.setNickname(original.nickname());
        MegaCobble.LOGGER.info("[Mega Cobble] Reverted {} ({}) to normal form.",
            pokemon.getSpecies().getName(), pokemon.getUuid());
    }

    /** Reverts every mega-evolved Pokémon participating in the given battle. */
    public static void revertBattle(PokemonBattle battle) {
        for (BattleActor actor : battle.getActors()) {
            for (BattlePokemon battlePokemon : actor.getPokemonList()) {
                revert(battlePokemon.getEffectedPokemon());
            }
        }
    }

    /** Finds the form with the given name (case-insensitive), e.g. "Mega", "Mega-X". */
    public static FormData findFormByName(Pokemon pokemon, String formName) {
        for (FormData form : pokemon.getSpecies().getForms()) {
            if (form.getName().equalsIgnoreCase(formName)) {
                return form;
            }
        }
        return null;
    }

    /**
     * Builds the transformed display name using the species' default (correctly-cased) name:
     * {@code Mega-<Species>[-X|-Y|-Z]}. "Mega" -> "Mega-Venusaur"; "Mega-X" -> "Mega-Charizard-X".
     */
    public static String buildMegaName(String speciesName, String formName) {
        String suffix = "";
        int dash = formName.indexOf('-');
        if (dash >= 0 && dash + 1 < formName.length()) {
            suffix = "-" + formName.substring(dash + 1).toUpperCase(Locale.ROOT); // "-X", "-Y", "-Z"
        }
        return "Mega-" + speciesName + suffix;
    }

    public static String buildMegaName(Pokemon pokemon, FormData megaForm) {
        return buildMegaName(pokemon.getSpecies().getName(), megaForm.getName());
    }
}
