package com.aaroncraft.megacobble.mega;

import com.aaroncraft.megacobble.MegaCobble;
import com.cobblemon.mod.common.api.abilities.AbilityTemplate;
import com.cobblemon.mod.common.api.abilities.PotentialAbility;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.battles.runner.ShowdownService;
import com.cobblemon.mod.common.pokemon.FormData;
import com.cobblemon.mod.common.pokemon.Species;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
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
        ShowdownService service;
        try {
            service = ShowdownService.Companion.getService();
        } catch (Throwable t) {
            MegaCobble.LOGGER.error("[Mega Cobble] Could not reach the Showdown sim to inject custom-mega data", t);
            return;
        }
        // Inject each registry type independently: a failure in one (e.g. an unknown type) must not
        // abort the others — notably the heldItem stone defs that custom megas need to trigger at all.
        for (Map.Entry<String, Map<String, String>> typeEntry : INJECTIONS.entrySet()) {
            try {
                service.sendRegistryData(new HashMap<>(typeEntry.getValue()), typeEntry.getKey());
            } catch (Throwable t) {
                MegaCobble.LOGGER.error("[Mega Cobble] Failed to inject '{}' data into the Showdown sim",
                    typeEntry.getKey(), t);
            }
        }
    }

    /**
     * Restores Mega-forme abilities in the running sim.
     *
     * <p>Cobblemon serializes every Mega forme to the battle sim with its abilities blanked to
     * "No Ability" (verified for all megas — classic and custom). So when a Pokémon mega evolves,
     * Showdown's forme change sets its ability from the mega species' {@code abilities[0]}, which is
     * "No Ability" — the mega's real ability (Speed Boost, Thick Fat, our custom Dragonize, …) never
     * takes effect, even though Cobblemon still shows the right name on the Cobblemon side.</p>
     *
     * <p>Fix: read the sim's current species (correct stats/types, blanked abilities), and for every
     * Mega forme re-register it with {@code abilities[0]} set to the correct ability — read from
     * Cobblemon's own form data, the same source that drives the displayed name. The sim resolves
     * {@code dex.species.get} from this registry first (ahead of its cache), so the corrected ability
     * applies immediately. Run at battle start, before any mega can happen; idempotent.</p>
     */
    public static void fixMegaAbilities() {
        try {
            ShowdownService service = ShowdownService.Companion.getService();
            JsonArray allSpecies = service.getRegistryData("species");
            if (allSpecies == null) {
                return;
            }
            Map<String, String> fixes = new HashMap<>();
            for (JsonElement element : allSpecies) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject species = element.getAsJsonObject();
                String forme = asString(species.get("forme"));
                String baseSpecies = asString(species.get("baseSpecies"));
                if (forme == null || baseSpecies == null
                    || !forme.toLowerCase(Locale.ROOT).startsWith("mega")) {
                    continue;
                }
                String abilityId = megaAbilityId(baseSpecies, forme);
                if (abilityId == null) {
                    continue;
                }
                JsonObject abilities = new JsonObject();
                abilities.addProperty("0", abilityId);
                species.add("abilities", abilities);
                String name = asString(species.get("name"));
                fixes.put(showdownId(name != null ? name : baseSpecies + forme), species.toString());
            }
            if (!fixes.isEmpty()) {
                service.sendRegistryData(fixes, "species");
                MegaCobble.LOGGER.info("[Mega Cobble] Restored in-battle abilities for {} mega forme(s).",
                    fixes.size());
            }
        } catch (Throwable t) {
            MegaCobble.LOGGER.error("[Mega Cobble] Failed to restore mega-forme abilities in the Showdown sim", t);
        }
    }

    /** The Showdown ability id for a mega forme, read from Cobblemon's own form data, or null. */
    private static String megaAbilityId(String baseSpecies, String forme) {
        Species species = PokemonSpecies.getByName(showdownId(baseSpecies));
        if (species == null) {
            return null;
        }
        String target = showdownId(forme);
        for (FormData form : species.getForms()) {
            if (!showdownId(form.getName()).equals(target)) {
                continue;
            }
            for (PotentialAbility potential : form.getAbilities()) {
                AbilityTemplate template = potential.getTemplate();
                if (template != null) {
                    return showdownId(template.getName());
                }
            }
        }
        return null;
    }

    private static String asString(JsonElement element) {
        return element == null || element.isJsonNull() ? null : element.getAsString();
    }

    /** Showdown's id normalization: lowercase, strip every non-alphanumeric character. */
    private static String showdownId(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
}
