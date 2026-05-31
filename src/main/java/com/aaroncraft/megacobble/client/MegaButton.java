package com.aaroncraft.megacobble.client;

import com.aaroncraft.megacobble.MegaCobble;
import com.aaroncraft.megacobble.item.MegaStones;
import com.aaroncraft.megacobble.item.ModItems;
import com.aaroncraft.megacobble.mega.MegaEvolution;
import com.aaroncraft.megacobble.net.MegaEvolvePayload;
import com.cobblemon.mod.common.client.CobblemonClient;
import com.cobblemon.mod.common.client.battle.ActiveClientBattlePokemon;
import com.cobblemon.mod.common.client.battle.ClientBattlePokemon;
import com.cobblemon.mod.common.client.battle.SingleActionRequest;
import com.cobblemon.mod.common.client.gui.battle.subscreen.BattleMoveSelection;
import com.cobblemon.mod.common.pokemon.Pokemon;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Renders and handles the "Mega Evolve" button on Cobblemon's Fight (move-selection) screen.
 *
 * <p>Flow (mirrors the native gimmick buttons): clicking the button toggles mega <b>armed</b>
 * on/off. Clicking the back button also disarms. The transformation is only sent when the player
 * <b>chooses a move</b> while armed. Once committed, the button is locked "on" and disabled for the
 * rest of the battle.</p>
 */
public final class MegaButton {

    private MegaButton() {}

    // Cobblemon's gimmick-button texture (36 x 68 — default state on top, on/hover state on bottom).
    private static final ResourceLocation TEXTURE =
        ResourceLocation.fromNamespaceAndPath("cobblemon", "textures/gui/battle/battle_gimmick_mega.png");

    // Match the native gimmick button: 36x34 source, drawn at 0.5 scale => 18x17 on screen.
    private static final int DRAW_W = 18;
    private static final int DRAW_H = 17;
    // Sit just right of the back button with the same gap Cobblemon uses between Fight/Switch
    // (BattleGUI.OPTION_HORIZONTAL_SPACING = 3). Back button ends at x=38; +3 px => x=41.
    private static final int X = 41;

    // Per-battle client state, reset whenever the active battle changes.
    private static Object trackedBattle = null;
    private static final Set<UUID> armed = new HashSet<>();          // button toggled on, awaiting a move
    private static final Set<UUID> usedThisBattle = new HashSet<>(); // mega already committed this battle

    private static void syncBattle() {
        Object battle = CobblemonClient.INSTANCE.getBattle();
        if (battle != trackedBattle) {
            trackedBattle = battle;
            armed.clear();
            usedThisBattle.clear();
        }
    }

    private static int y() {
        return Minecraft.getInstance().getWindow().getGuiScaledHeight() - 22;
    }

    public static void render(BattleMoveSelection selection, GuiGraphics context, int mouseX, int mouseY) {
        syncBattle();
        if (!shouldShow(selection)) {
            return;
        }
        Pokemon pokemon = actingPokemon(selection);
        // "On" (bottom of texture) when selected/locked or hovered; default (top) otherwise.
        boolean on = (pokemon != null && isLockedOn(pokemon)) || isHovered(mouseX, mouseY);
        context.blit(TEXTURE, X, y(), DRAW_W, DRAW_H, 0F, on ? 34F : 0F, 36, 34, 36, 68);
    }

    /**
     * Handle a click on the Mega button itself. Returns true to consume the click.
     * Toggles armed on/off. If mega has already been committed this battle the button is disabled.
     */
    public static boolean handleButtonClick(BattleMoveSelection selection, double mouseX, double mouseY) {
        syncBattle();
        if (!shouldShow(selection) || !isHovered(mouseX, mouseY)) {
            return false;
        }
        Pokemon pokemon = actingPokemon(selection);
        if (pokemon == null) {
            return false;
        }
        UUID id = pokemon.getUuid();
        if (usedThisBattle.contains(id) || isMega(pokemon)) {
            return true; // already mega evolved this battle -> disabled, consume the click
        }
        if (armed.remove(id)) {
            MegaCobble.LOGGER.info("[Mega Cobble] Mega de-selected for {}.", pokemon.getSpecies().getName());
        } else {
            armed.add(id);
            MegaCobble.LOGGER.info("[Mega Cobble] Mega armed for {} - choose a move to mega evolve.",
                pokemon.getSpecies().getName());
        }
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.ANVIL_LAND, 1.0F));
        return true;
    }

    /**
     * Called for non-button clicks. If mega is armed: clicking the back button disarms; clicking a
     * move tile commits the transformation (sent alongside the normal move selection).
     */
    public static void handleNonButtonClick(BattleMoveSelection selection, double mouseX, double mouseY) {
        syncBattle();
        Pokemon pokemon = actingPokemon(selection);
        if (pokemon == null || !armed.contains(pokemon.getUuid())) {
            return;
        }
        // Back button -> de-select mega.
        if (selection.getBackButton().isHovered(mouseX, mouseY)) {
            armed.remove(pokemon.getUuid());
            MegaCobble.LOGGER.info("[Mega Cobble] Mega de-selected (back) for {}.", pokemon.getSpecies().getName());
            return;
        }
        // Move tile -> commit mega evolution.
        for (BattleMoveSelection.MoveTile tile : selection.getMoveTiles()) {
            if (tile.isHovered(mouseX, mouseY)) {
                commit(selection, pokemon);
                return;
            }
        }
    }

    private static void commit(BattleMoveSelection selection, Pokemon pokemon) {
        UUID id = pokemon.getUuid();
        armed.remove(id);
        usedThisBattle.add(id);
        updateBattleMenu(selection, pokemon);
        MegaCobble.LOGGER.info("[Mega Cobble] Move chosen while mega armed -> sending mega-evolve for {} ({}).",
            pokemon.getSpecies().getName(), id);
        ClientPlayNetworking.send(new MegaEvolvePayload(id));
    }

    /** Optimistically update the in-battle name and portrait (the battle menu reads these, not the live party). */
    private static void updateBattleMenu(BattleMoveSelection selection, Pokemon pokemon) {
        ClientBattlePokemon battlePokemon = selection.getRequest().getActivePokemon().getBattlePokemon();
        MegaStones.MegaStone stone = MegaStones.byItem(pokemon.heldItem().getItem());
        if (battlePokemon == null || stone == null) {
            return;
        }
        battlePokemon.setDisplayName(Component.literal(
            MegaEvolution.buildMegaName(pokemon.getSpecies().getName(), stone.form())));
        Set<String> aspects = new HashSet<>(battlePokemon.getState().getCurrentAspects());
        aspects.add(stone.aspect());
        battlePokemon.updateAspects(aspects);
    }

    /** Locked-on = armed this turn, already used this battle, or already in a mega form. */
    private static boolean isLockedOn(Pokemon pokemon) {
        return armed.contains(pokemon.getUuid())
            || usedThisBattle.contains(pokemon.getUuid())
            || isMega(pokemon);
    }

    private static boolean isMega(Pokemon pokemon) {
        return pokemon.getForm().getName().toLowerCase(Locale.ROOT).startsWith("mega");
    }

    private static boolean isHovered(double mouseX, double mouseY) {
        int top = y();
        return mouseX >= X && mouseX <= X + DRAW_W && mouseY >= top && mouseY <= top + DRAW_H;
    }

    /** Show only when: the acting Pokémon holds its matching Mega Stone and the player holds a Key Stone. */
    private static boolean shouldShow(BattleMoveSelection selection) {
        Pokemon pokemon = actingPokemon(selection);
        if (pokemon == null) {
            return false;
        }
        MegaStones.MegaStone stone = MegaStones.byItem(pokemon.heldItem().getItem());
        if (stone == null || !stone.species().equalsIgnoreCase(pokemon.getSpecies().getName())) {
            return false;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && player.getInventory().hasAnyMatching(stack -> stack.is(ModItems.KEY_STONE));
    }

    /** Resolves the real Pokémon currently choosing a move on this screen. */
    private static Pokemon actingPokemon(BattleMoveSelection selection) {
        SingleActionRequest request = selection.getRequest();
        ActiveClientBattlePokemon active = request.getActivePokemon();
        ClientBattlePokemon battlePokemon = active.getBattlePokemon();
        if (battlePokemon == null) {
            return null;
        }
        for (Pokemon pokemon : active.getActor().getPokemon()) {
            if (pokemon.getUuid().equals(battlePokemon.getUuid())) {
                return pokemon;
            }
        }
        return null;
    }
}
