package com.aaroncraft.megacobble.variant;

import com.aaroncraft.megacobble.MegaCobble;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Named visual variants loaded from the bundled {@code /variants.json}. A variant is just a friendly,
 * tab-completable name for a set of forced aspects; resolvers map those aspects to model / texture /
 * animation. {@code kind} distinguishes a bundled {@code look} (model + texture + animation together)
 * from an independent axis ({@code model} / {@code texture} / {@code animation}) that layers on top.
 *
 * <p>This is the catalog behind {@code /megacobble variant}; the low-level {@code /megacobble aspect}
 * command can still push any raw aspect string.</p>
 */
public final class MegaVariants {

    private MegaVariants() {}

    /**
     * A named look. {@code species} (nullable) restricts which species it's meant for (null = any);
     * {@code aspects} are applied as forced aspects, which the resource pack's resolvers map to a
     * model / texture / animation.
     */
    public record Variant(String id, String label, String kind, String species, List<String> aspects) {}

    private static final Map<String, Variant> BY_ID = new LinkedHashMap<>();

    public static List<Variant> all() {
        return new ArrayList<>(BY_ID.values());
    }

    /** @return the registered variant with this id, or null. */
    public static Variant byId(String id) {
        return BY_ID.get(id);
    }

    /** @return all registered variant ids (for command suggestions), in file order. */
    public static Iterable<String> ids() {
        return BY_ID.keySet();
    }

    /** Optional admin overlay catalog: {@code config/megacobble/variants.json} (adds/overrides). */
    private static Path overlayPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(MegaCobble.MOD_ID).resolve("variants.json");
    }

    /**
     * Loads the bundled (generated) catalog as the base, then merges an optional on-disk overlay at
     * {@code config/megacobble/variants.json} on top (entries with the same id win). So generated
     * looks always show, and an admin can add/override without rebuilding.
     */
    public static void load() {
        BY_ID.clear();
        parseInto(readBundled());
        parseInto(readOverlay());
        MegaCobble.LOGGER.info("[Mega Cobble] Loaded {} visual variants.", BY_ID.size());
    }

    private static void parseInto(JsonObject root) {
        if (root == null) {
            return;
        }
        JsonArray variants = root.getAsJsonArray("variants");
        if (variants == null) {
            return;
        }
        for (JsonElement element : variants) {
            JsonObject o = element.getAsJsonObject();
            String id = o.get("id").getAsString();
            String label = o.has("label") ? o.get("label").getAsString() : id;
            String kind = o.has("kind") ? o.get("kind").getAsString() : "look";
            String species = o.has("species") ? o.get("species").getAsString() : null;
            List<String> aspects = new ArrayList<>();
            for (JsonElement a : o.getAsJsonArray("aspects")) {
                aspects.add(a.getAsString());
            }
            BY_ID.put(id, new Variant(id, label, kind, species, aspects));
        }
    }

    /** The catalog shipped in the mod (generated) — always the base. */
    private static JsonObject readBundled() {
        try (InputStream in = MegaVariants.class.getResourceAsStream("/variants.json");
             InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception e) {
            MegaCobble.LOGGER.error("[Mega Cobble] Failed to load bundled variants.json", e);
            return null;
        }
    }

    /** Optional on-disk overlay that adds/overrides catalog entries, or null if absent. */
    private static JsonObject readOverlay() {
        Path path = overlayPath();
        if (!Files.exists(path)) {
            return null;
        }
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception e) {
            MegaCobble.LOGGER.error("[Mega Cobble] Failed to read overlay {}", path, e);
            return null;
        }
    }
}
