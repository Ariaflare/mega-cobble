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
import java.util.Locale;
import java.util.Map;

/**
 * Loads the bundled {@code /megastones.json} manifest. Each Mega Stone is a definition only — the
 * actual in-world item is a vanilla item tagged via {@link MegaItems} — so the same generic transform
 * handles every stone (including Charizard/Mewtwo X &amp; Y) and the stones work for clients without
 * the mod installed.
 */
public final class MegaStones {

    private MegaStones() {}

    /**
     * A Mega Stone and the species/form it unlocks.
     * {@code stoneId} is the manifest key (e.g. "venusaurite"), used as the {@code custom_data} id.
     * {@code showdownId} is the lowercase-alphanumeric id the Pokémon Showdown sim uses for the stone.
     * {@code requiredAspect} (nullable) restricts the stone to a specific form aspect — e.g. Floettite
     * only works on the {@code flower-eternal} (Eternal Flower) Floette.
     */
    public record MegaStone(String stoneId, String name, String species, String form, String aspect,
                            String requiredAspect, String showdownId, int customModelData) {}

    private static final Map<String, MegaStone> BY_STONE_ID = new LinkedHashMap<>();
    private static final Map<String, MegaStone> BY_SHOWDOWN_ID = new java.util.HashMap<>();

    public static List<MegaStone> all() {
        return new ArrayList<>(BY_STONE_ID.values());
    }

    /** @return the Mega Stone with this manifest/custom_data id, or null. */
    public static MegaStone byStoneId(String stoneId) {
        return stoneId == null ? null : BY_STONE_ID.get(stoneId);
    }

    /** @return the Mega Stone carried in the stack's custom_data, or null if it isn't a Mega Stone. */
    public static MegaStone byCustomData(ItemStack stack) {
        return byStoneId(MegaItems.idOf(stack));
    }

    /** @return the Mega Stone with the given Showdown id, or null. */
    public static MegaStone byShowdownId(String showdownId) {
        return BY_SHOWDOWN_ID.get(showdownId);
    }

    /** @return every stone id (for command suggestions), in manifest order. */
    public static Iterable<String> stoneIds() {
        return BY_STONE_ID.keySet();
    }

    public static void load() {
        BY_STONE_ID.clear();
        BY_SHOWDOWN_ID.clear();
        JsonArray entries;
        try (InputStream in = MegaStones.class.getResourceAsStream("/megastones.json");
             InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            entries = JsonParser.parseReader(reader).getAsJsonArray();
        } catch (Exception e) {
            MegaCobble.LOGGER.error("[Mega Cobble] Failed to load megastones.json", e);
            return;
        }

        // custom_model_data hook for an external resource pack to re-skin each stone. 1 is reserved
        // for the Key Stone (see MegaItems), so manifest stones start at 2, in file order.
        int customModelData = 2;
        for (JsonElement element : entries) {
            JsonObject o = element.getAsJsonObject();
            String stoneId = o.get("stone").getAsString();
            String name = o.get("name").getAsString();
            String showdownId = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
            String requiredAspect = o.has("requiredAspect") ? o.get("requiredAspect").getAsString() : null;
            MegaStone stone = new MegaStone(
                stoneId,
                name,
                o.get("species").getAsString(),
                o.get("form").getAsString(),
                o.get("aspect").getAsString(),
                requiredAspect,
                showdownId,
                customModelData++);
            BY_STONE_ID.put(stoneId, stone);
            BY_SHOWDOWN_ID.put(showdownId, stone);
        }
        MegaCobble.LOGGER.info("[Mega Cobble] Loaded {} mega stones.", BY_STONE_ID.size());
    }
}
