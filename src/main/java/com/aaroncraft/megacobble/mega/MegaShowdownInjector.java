package com.aaroncraft.megacobble.mega;

import com.aaroncraft.megacobble.MegaCobble;
import com.cobblemon.mod.common.battles.runner.ShowdownService;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Injects custom-mega data into Cobblemon's bundled Pokémon Showdown sim at runtime.
 *
 * <p>Cobblemon's bundled sim only knows the classic Gen 6/ORAS megas. For Legends Z-A and fully
 * custom megas, the mega-stone item (and any new ability) don't exist in the sim, and Cobblemon
 * won't send them. This pushes them through the same channel Cobblemon uses for its own data
 * ({@code ShowdownService.sendRegistryData}), reading the definitions from
 * {@code /custom_mega_showdown.json}.</p>
 *
 * <p>Injection is re-run at the start of each battle: by then the sim is booted and Cobblemon has
 * synced its own data, and re-sending our handful of entries is cheap, idempotent, and survives a
 * {@code /reloadshowdown}.</p>
 */
public final class MegaShowdownInjector {

    private MegaShowdownInjector() {}

    // registry type -> (id -> JS definition string)
    private static final Map<String, Map<String, String>> INJECTIONS = new LinkedHashMap<>();

    /** Loads the injection definitions from the bundled JSON. Call once at init. */
    public static void load() {
        INJECTIONS.clear();
        JsonObject root;
        try (InputStream in = MegaShowdownInjector.class.getResourceAsStream("/custom_mega_showdown.json");
             InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            root = JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception e) {
            MegaCobble.LOGGER.error("[Mega Cobble] Failed to load custom_mega_showdown.json", e);
            return;
        }

        for (Map.Entry<String, JsonElement> typeEntry : root.entrySet()) {
            String type = typeEntry.getKey();
            if (type.startsWith("_") || !typeEntry.getValue().isJsonObject()) {
                continue; // skip comments / non-registry keys
            }
            Map<String, String> entries = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> e : typeEntry.getValue().getAsJsonObject().entrySet()) {
                entries.put(e.getKey(), e.getValue().getAsString());
            }
            if (!entries.isEmpty()) {
                INJECTIONS.put(type, entries);
            }
        }
        MegaCobble.LOGGER.info("[Mega Cobble] Loaded custom-mega sim injections: {}.", INJECTIONS.keySet());
    }

    /** Pushes every injection into the running sim. Safe to call repeatedly (e.g. each battle start). */
    public static void injectAll() {
        if (INJECTIONS.isEmpty()) {
            return;
        }
        try {
            ShowdownService service = ShowdownService.Companion.getService();
            for (Map.Entry<String, Map<String, String>> typeEntry : INJECTIONS.entrySet()) {
                service.sendRegistryData(new HashMap<>(typeEntry.getValue()), typeEntry.getKey());
            }
        } catch (Throwable t) {
            MegaCobble.LOGGER.error("[Mega Cobble] Failed to inject custom-mega data into the Showdown sim", t);
        }
    }
}
