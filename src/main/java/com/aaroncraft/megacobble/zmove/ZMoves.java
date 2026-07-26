package com.aaroncraft.megacobble.zmove;

import com.aaroncraft.megacobble.MegaCobble;
import com.aaroncraft.megacobble.item.MegaItems;
import com.cobblemon.mod.common.Cobblemon;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Set;

/**
 * Z-Move glue around Cobblemon's native (Showdown-driven) Z-Power gimmick.
 *
 * <p>Cobblemon + its bundled Showdown sim already implement Z-Moves end to end: every Z-Crystal and
 * Z-Move is standard sim data, the sim computes the Z-Move (power, effects) and enforces "one Z-Move
 * per side per battle", and Cobblemon draws the in-battle Z button. The button, however, only appears
 * when the trainer holds Cobblemon's abstract {@code cobblemon:z_ring} key item — exactly parallel to
 * how Mega Evolution is gated behind {@code cobblemon:key_stone}.</p>
 *
 * <p>So our entire job for Z-Moves is two small bridges: expose held Z-Crystals to the sim (see
 * {@link ZCrystalHeldItemManager}) and, here, bridge our Z-Ring item to that key-item gate. Unlike
 * Mega Evolution there is no form change, no revert, and no out-of-battle variant — a Z-Move is a
 * one-shot attack, so this class is only the key-item sync.</p>
 */
public final class ZMoves {

    private ZMoves() {}

    /** Cobblemon's abstract key item that {@code ShowdownActionRequest.sanitize()} checks for Z-Power. */
    private static final ResourceLocation Z_RING_KEY_ITEM =
        ResourceLocation.fromNamespaceAndPath("cobblemon", "z_ring");

    /**
     * Bridges our Z-Ring item to Cobblemon's native Z-Power gate. If the player is carrying a Z-Ring
     * in their inventory, grant the {@code cobblemon:z_ring} key item (so {@code sanitize()} lets the
     * Z button through); otherwise remove it. Called at battle start, mirroring
     * {@code MegaEvolution.syncKeyStone}.
     */
    public static void syncZRing(ServerPlayer player) {
        boolean hasZRing = player.getInventory().hasAnyMatching(MegaItems::isZRing);
        Set<ResourceLocation> keyItems =
            Cobblemon.INSTANCE.getPlayerDataManager().getGenericData(player).getKeyItems();
        if (hasZRing) {
            keyItems.add(Z_RING_KEY_ITEM);
            MegaCobble.LOGGER.info("[Mega Cobble] {} brought a Z-Ring -> Z-Moves enabled this battle.",
                player.getGameProfile().getName());
        } else {
            keyItems.remove(Z_RING_KEY_ITEM);
        }
    }
}
