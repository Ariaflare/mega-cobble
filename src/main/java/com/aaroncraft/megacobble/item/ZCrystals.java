package com.aaroncraft.megacobble.item;

import com.aaroncraft.megacobble.MegaCobble;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.world.item.ItemStack;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads the bundled {@code /zcrystals.json} manifest of Z-Crystals.
 *
 * <p>Like {@link MegaStones}, a crystal is a definition only — the in-world item is a vanilla item
 * tagged via {@link MegaItems} — so the same generic transform handles every crystal and they work
 * for clients without the mod. The heavy lifting (turning a held crystal + a Z-move into the actual
 * Z-Move) is done entirely by Cobblemon's bundled Pokémon Showdown sim, which already knows every
 * crystal and Z-Move; our job is only to expose the held crystal to the sim (see
 * {@code ZCrystalHeldItemManager}) and to gate the Z button behind a Z-Ring (see {@code ZMoves}).</p>
 *
 * <p>The manifest {@code crystal} id is deliberately the same string the Showdown sim uses for the
 * item (e.g. {@code "electriumz"}), so the {@code custom_data} id, our lookup key, and the sim id are
 * all one and the same.</p>
 */
public final class ZCrystals {

    private ZCrystals() {}

    /**
     * A Z-Crystal and what it unlocks.
     * {@code crystalId} is the manifest key + {@code custom_data} id + Showdown item id (all equal).
     * For a generic <b>type</b> crystal, {@code type} is set (e.g. "electric") and {@code species}/
     * {@code fromMove} are null. For a <b>signature</b> crystal, {@code species} (the Cobblemon
     * species path, informational) and {@code fromMove} (the base move it upgrades) are set and
     * {@code type} is null. The species lock for signature crystals is enforced by the sim itself
     * (via the item's {@code itemUser}); the fields here are for commands / docs / display.
     */
    public record ZCrystal(String crystalId, String name, String type, String species, String fromMove,
                           int customModelData) {

        /** @return true if this is a generic per-type crystal (vs a species-specific signature one). */
        public boolean isTypeCrystal() {
            return type != null;
        }

        /** @return the Showdown item id (identical to {@link #crystalId()}). */
        public String showdownId() {
            return crystalId;
        }
    }

    private static final Map<String, ZCrystal> BY_CRYSTAL_ID = new LinkedHashMap<>();
    private static final Map<String, ZCrystal> BY_SHOWDOWN_ID = new java.util.HashMap<>();

    public static List<ZCrystal> all() {
        return new ArrayList<>(BY_CRYSTAL_ID.values());
    }

    /** @return the Z-Crystal with this manifest/custom_data id, or null. */
    public static ZCrystal byCrystalId(String crystalId) {
        return crystalId == null ? null : BY_CRYSTAL_ID.get(crystalId);
    }

    /** @return the Z-Crystal carried in the stack's custom_data, or null if it isn't a Z-Crystal. */
    public static ZCrystal byCustomData(ItemStack stack) {
        return byCrystalId(MegaItems.idOf(stack));
    }

    /** @return the Z-Crystal with the given Showdown id, or null. */
    public static ZCrystal byShowdownId(String showdownId) {
        return BY_SHOWDOWN_ID.get(showdownId);
    }

    /** @return every crystal id (for command suggestions), in manifest order. */
    public static Iterable<String> crystalIds() {
        return BY_CRYSTAL_ID.keySet();
    }

    public static void load() {
        BY_CRYSTAL_ID.clear();
        BY_SHOWDOWN_ID.clear();
        JsonArray entries;
        try (InputStream in = ZCrystals.class.getResourceAsStream("/zcrystals.json");
             InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            entries = JsonParser.parseReader(reader).getAsJsonArray();
        } catch (Exception e) {
            MegaCobble.LOGGER.error("[Mega Cobble] Failed to load zcrystals.json", e);
            return;
        }

        // custom_model_data hook for an external resource pack to re-skin each crystal. The Z-Ring
        // reserves MegaItems.Z_RING_CMD; crystals start just after it, in manifest order. Kept in a
        // separate range from the Mega Stones so a pack can skin both without collisions.
        int customModelData = MegaItems.Z_CRYSTAL_CMD_START;
        for (JsonElement element : entries) {
            JsonObject o = element.getAsJsonObject();
            String crystalId = o.get("crystal").getAsString();
            String name = o.get("name").getAsString();
            String type = o.has("type") ? o.get("type").getAsString() : null;
            String species = o.has("species") ? o.get("species").getAsString() : null;
            String fromMove = o.has("from") ? o.get("from").getAsString() : null;
            ZCrystal crystal = new ZCrystal(crystalId, name, type, species, fromMove, customModelData++);
            BY_CRYSTAL_ID.put(crystalId, crystal);
            BY_SHOWDOWN_ID.put(crystal.showdownId(), crystal);
        }
        MegaCobble.LOGGER.info("[Mega Cobble] Loaded {} Z-Crystals.", BY_CRYSTAL_ID.size());
    }
}
