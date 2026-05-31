package com.aaroncraft.megacobble.mega;

import com.aaroncraft.megacobble.MegaCobble;
import com.aaroncraft.megacobble.item.MegaStones;
import com.aaroncraft.megacobble.item.ModItems;
import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.pokemon.FormData;
import com.cobblemon.mod.common.pokemon.Pokemon;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mega Evolution glue around Cobblemon's native (Showdown-driven) mega.
 *
 * <p>Showdown computes the real in-battle mega (stats, typing, ability) for the classic mega-capable
 * species. Our job is two-fold:</p>
 * <ul>
 *   <li>{@link #syncKeyStone} — bridge our Key Stone item to Cobblemon's key-item gate so the native
 *       mega button appears only when the player brought a Key Stone.</li>
 *   <li>{@link #applyMega} — when Showdown mega evolves a Pokémon, mirror the form on the Minecraft
 *       side (model / name), and {@link #revert} it when the battle ends.</li>
 * </ul>
 */
public final class MegaEvolution {

    private MegaEvolution() {}

    /** Cobblemon's abstract key-item flag that {@code ShowdownActionRequest.sanitize()} checks for mega. */
    private static final ResourceLocation KEY_STONE_KEY_ITEM =
        ResourceLocation.fromNamespaceAndPath("cobblemon", "key_stone");

    /** Pre-mega state kept so a Pokémon can be reverted at battle end / faint / flee. */
    private static final Map<UUID, Original> MEGA_STATES = new ConcurrentHashMap<>();

    private record Original(Set<String> forcedAspects, MutableComponent nickname) {}

    /**
     * Bridges our Key Stone item to Cobblemon's native mega gate. If the player is carrying a Key
     * Stone in their inventory, grant the {@code cobblemon:key_stone} key item (so {@code sanitize()}
     * lets the native mega gimmick through); otherwise remove it. Called at battle start.
     */
    public static void syncKeyStone(ServerPlayer player) {
        boolean hasKeyStone = player.getInventory().hasAnyMatching(stack -> stack.is(ModItems.KEY_STONE));
        Set<ResourceLocation> keyItems =
            Cobblemon.INSTANCE.getPlayerDataManager().getGenericData(player).getKeyItems();
        if (hasKeyStone) {
            keyItems.add(KEY_STONE_KEY_ITEM);
            MegaCobble.LOGGER.info("[Mega Cobble] {} brought a Key Stone -> mega enabled this battle.",
                player.getGameProfile().getName());
        } else {
            keyItems.remove(KEY_STONE_KEY_ITEM);
        }
    }

    /**
     * Mirrors a Showdown mega evolution onto the Minecraft side: forces the mega aspect (which drives
     * the rendered model and re-selects the mega form) and renames the Pokémon. Battle stats/typing/
     * ability are Showdown's job; this is the visual + data form. The pre-mega state is recorded for
     * {@link #revert}.
     */
    public static void applyMega(Pokemon target) {
        MegaStones.MegaStone stone = MegaStones.byItem(target.heldItem().getItem());
        if (stone == null) {
            return;
        }
        FormData megaForm = findFormByName(target, stone.form());
        if (megaForm == null) {
            return;
        }
        MEGA_STATES.put(target.getUuid(),
            new Original(new HashSet<>(target.getForcedAspects()), target.getNickname()));

        Set<String> forced = new HashSet<>(target.getForcedAspects());
        forced.addAll(megaForm.getAspects());
        target.setForcedAspects(forced);
        target.setForm(megaForm);
        target.setNickname(Component.literal(buildMegaName(target.getSpecies().getName(), megaForm.getName())));

        MegaCobble.LOGGER.info("[Mega Cobble] In-battle mega: {} -> {} ({}).",
            target.getSpecies().getName(), megaForm.getName(), target.getUuid());
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
}
