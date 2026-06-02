package com.aaroncraft.megacobble.skin;

import com.aaroncraft.megacobble.MegaCobble;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.pokemon.aspect.AspectProvider;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A server-wide, per-species skin override. Registered as a Cobblemon {@link AspectProvider}: for any
 * Pokémon whose species has a global skin set, the server adds the mapped aspect when it computes the
 * Pokémon's aspects, then syncs that to the client — so <em>every</em> Pokémon of that species renders
 * the skin (loaded, boxed, or caught later), with no client mod beyond the resolver/pack.
 *
 * <p>The mapping (species id → skin aspect) is persisted to {@code config/megacobble/global_skins.json}
 * and edited via {@code /megacobble skin set|clear <aspect> all}.</p>
 */
public final class GlobalSkins implements AspectProvider {

    public static final GlobalSkins INSTANCE = new GlobalSkins();

    /** species path (e.g. "venusaur") -> skin aspect. */
    private static final Map<String, String> MAP = new ConcurrentHashMap<>();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private GlobalSkins() {}

    /** Loads the saved mappings and registers the aspect provider with Cobblemon. */
    public static void init() {
        load();
        AspectProvider.Companion.register(INSTANCE);
        MegaCobble.LOGGER.info("[Mega Cobble] Global skins active for {} species.", MAP.size());
    }

    @Override
    public Set<String> provide(Pokemon pokemon) {
        String aspect = MAP.get(pokemon.getSpecies().getResourceIdentifier().getPath());
        return aspect == null ? Set.of() : Set.of(aspect);
    }

    @Override
    public Set<String> provide(PokemonProperties properties) {
        String species = properties.getSpecies();
        if (species == null) {
            return Set.of();
        }
        int colon = species.indexOf(':');
        if (colon >= 0) {
            species = species.substring(colon + 1);
        }
        String aspect = MAP.get(species.toLowerCase(Locale.ROOT));
        return aspect == null ? Set.of() : Set.of(aspect);
    }

    /** @return the global skin aspect set for this species, or null. */
    public static String get(String species) {
        return MAP.get(species);
    }

    /** Sets the global skin for a species and persists. */
    public static void set(String species, String aspect) {
        MAP.put(species, aspect);
        save();
    }

    /** Clears the global skin for a species and persists. @return true if one was set. */
    public static boolean clear(String species) {
        boolean removed = MAP.remove(species) != null;
        if (removed) {
            save();
        }
        return removed;
    }

    public static Map<String, String> all() {
        return new LinkedHashMap<>(MAP);
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve(MegaCobble.MOD_ID).resolve("global_skins.json");
    }

    private static void load() {
        MAP.clear();
        Path path = path();
        if (!Files.exists(path)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            Map<String, String> loaded = GSON.fromJson(reader, new TypeToken<Map<String, String>>() {}.getType());
            if (loaded != null) {
                MAP.putAll(loaded);
            }
        } catch (Exception e) {
            MegaCobble.LOGGER.error("[Mega Cobble] Failed to read {}", path, e);
        }
    }

    private static void save() {
        Path path = path();
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(MAP, writer);
            }
        } catch (Exception e) {
            MegaCobble.LOGGER.error("[Mega Cobble] Failed to write {}", path, e);
        }
    }
}
