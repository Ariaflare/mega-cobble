package com.aaroncraft.megacobble.item;

import com.aaroncraft.megacobble.MegaCobble;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads the bundled {@code /megastones.json} manifest and registers one item per Mega Stone.
 * Each stone knows the species and the specific mega form (and aspect) it enables, so the same
 * generic transform handles every stone, including Charizard/Mewtwo X &amp; Y.
 */
public final class MegaStones {

    private MegaStones() {}

    /**
     * A registered Mega Stone and the species/form it unlocks.
     * {@code showdownId} is the lowercase-alphanumeric id the Pokémon Showdown sim uses for the
     * stone (e.g. "venusaurite", "charizarditex"), used to expose the stone to the battle sim.
     */
    public record MegaStone(String stoneId, String name, String species, String form, String aspect,
                            String showdownId, Item item) {}

    private static final List<MegaStone> ALL = new ArrayList<>();
    private static final Map<Item, MegaStone> BY_ITEM = new HashMap<>();
    private static final Map<String, MegaStone> BY_SHOWDOWN_ID = new HashMap<>();

    public static List<MegaStone> all() {
        return ALL;
    }

    /** @return the Mega Stone this item is, or null if the item isn't a Mega Stone. */
    public static MegaStone byItem(Item item) {
        return BY_ITEM.get(item);
    }

    /** @return the Mega Stone with the given Showdown id, or null. */
    public static MegaStone byShowdownId(String showdownId) {
        return BY_SHOWDOWN_ID.get(showdownId);
    }

    public static void registerAll() {
        JsonArray entries;
        try (InputStream in = MegaStones.class.getResourceAsStream("/megastones.json");
             InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            entries = JsonParser.parseReader(reader).getAsJsonArray();
        } catch (Exception e) {
            MegaCobble.LOGGER.error("[Mega Cobble] Failed to load megastones.json", e);
            return;
        }

        for (JsonElement element : entries) {
            JsonObject o = element.getAsJsonObject();
            String stoneId = o.get("stone").getAsString();
            Item item = new Item(new Item.Properties().stacksTo(1));
            Registry.register(BuiltInRegistries.ITEM,
                ResourceLocation.fromNamespaceAndPath(MegaCobble.MOD_ID, stoneId), item);
            String name = o.get("name").getAsString();
            String showdownId = name.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]", "");
            MegaStone stone = new MegaStone(
                stoneId,
                name,
                o.get("species").getAsString(),
                o.get("form").getAsString(),
                o.get("aspect").getAsString(),
                showdownId,
                item);
            ALL.add(stone);
            BY_ITEM.put(item, stone);
            BY_SHOWDOWN_ID.put(showdownId, stone);
        }
        MegaCobble.LOGGER.info("[Mega Cobble] Registered {} mega stones.", ALL.size());
    }
}
