package com.aaroncraft.megacobble.config;

import com.aaroncraft.megacobble.MegaCobble;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Mega Cobble configuration, persisted to {@code config/megacobble.json}.
 *
 * <p>Controls the out-of-battle ("world") Mega Evolution feature: mega-evolving a Pokémon for
 * overworld exploration via the interaction wheel (modded clients) or the {@code /megacobble}
 * command (any client). The in-battle Showdown mega is unaffected by these toggles.</p>
 */
public final class MegaCobbleConfig {

    /** Master switch for mega-evolving outside of battle (wheel option + command). */
    public boolean worldMegaEnabled = true;
    /** Require the trainer to be carrying a Key Stone, mirroring the battle requirement. */
    public boolean requireKeyStone = true;
    /** Require the Pokémon to hold its matching Mega Stone, mirroring the battle requirement. */
    public boolean requireMegaStone = true;
    /**
     * When false, the "Mega Evolve" option is hidden from the interaction wheel — so players can't
     * mega-evolve (and then ride) a Pokémon in the overworld via the wheel. (The command still works.)
     */
    public boolean allowRideInMega = true;
    /** Revert any world-mega'd Pokémon when a battle starts, so the in-battle Showdown mega runs cleanly. */
    public boolean revertOnBattleStart = true;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static MegaCobbleConfig instance;

    /** The live config, loading it from disk (or defaults) on first access. */
    public static MegaCobbleConfig get() {
        if (instance == null) {
            load();
        }
        return instance;
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve(MegaCobble.MOD_ID + ".json");
    }

    /** (Re)loads the config from disk, falling back to defaults, and writes it back to normalise the file. */
    public static void load() {
        Path path = path();
        MegaCobbleConfig loaded = null;
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                loaded = GSON.fromJson(reader, MegaCobbleConfig.class);
            } catch (Exception e) {
                MegaCobble.LOGGER.error("[Mega Cobble] Failed to read {}, using defaults.", path, e);
            }
        }
        instance = loaded != null ? loaded : new MegaCobbleConfig();
        save();
    }

    /** Persists the current config to disk. */
    public static void save() {
        Path path = path();
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(instance, writer);
            }
        } catch (Exception e) {
            MegaCobble.LOGGER.error("[Mega Cobble] Failed to write {}.", path, e);
        }
    }

    /** The mutable boolean keys exposed to the {@code /megacobble config} command, in display order. */
    public static final String[] KEYS = {
        "worldMegaEnabled", "requireKeyStone", "requireMegaStone", "allowRideInMega", "revertOnBattleStart"
    };

    /** @return the current value of a boolean config key, or null if the key is unknown. */
    public Boolean getBool(String key) {
        switch (key.toLowerCase(Locale.ROOT)) {
            case "worldmegaenabled": return worldMegaEnabled;
            case "requirekeystone": return requireKeyStone;
            case "requiremegastone": return requireMegaStone;
            case "allowrideinmega": return allowRideInMega;
            case "revertonbattlestart": return revertOnBattleStart;
            default: return null;
        }
    }

    /** Sets a boolean config key and persists. @return false if the key is unknown. */
    public boolean setBool(String key, boolean value) {
        switch (key.toLowerCase(Locale.ROOT)) {
            case "worldmegaenabled": worldMegaEnabled = value; break;
            case "requirekeystone": requireKeyStone = value; break;
            case "requiremegastone": requireMegaStone = value; break;
            case "allowrideinmega": allowRideInMega = value; break;
            case "revertonbattlestart": revertOnBattleStart = value; break;
            default: return false;
        }
        save();
        return true;
    }
}
