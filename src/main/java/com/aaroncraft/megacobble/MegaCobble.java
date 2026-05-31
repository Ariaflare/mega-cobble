package com.aaroncraft.megacobble;

import com.aaroncraft.megacobble.item.ModItems;
import com.aaroncraft.megacobble.mega.MegaEvolution;
import com.aaroncraft.megacobble.mega.MegaShowdownInjector;
import com.aaroncraft.megacobble.mega.MegaStoneHeldItemManager;
import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.pokemon.helditem.HeldItemProvider;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mega Cobble - a Cobblemon add-on that adds Mega Evolution.
 *
 * <p>Common (server + client) entrypoint. Mega Evolution runs through Cobblemon's native, Showdown-
 * driven mega: we expose our Mega Stones to the sim, bridge our Key Stone item to Cobblemon's
 * key-item gate, mirror the form change visually when a Pokémon mega evolves, and revert it when the
 * battle ends.</p>
 */
public class MegaCobble implements ModInitializer {
	public static final String MOD_ID = "megacobble";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("[Mega Cobble] Initializing Mega Evolution for Cobblemon.");

		ModItems.register();
		MegaShowdownInjector.load();

		// Expose our Mega Stones to the bundled Showdown sim (Cobblemon's default manager only
		// handles cobblemon-namespace items). Higher priority than Cobblemon's LOWEST default.
		HeldItemProvider.register(new MegaStoneHeldItemManager(), Priority.NORMAL);

		// At battle start: inject custom-mega data into the sim (megas not in Cobblemon's bundled
		// sim), and bridge each player's Key Stone item to Cobblemon's key-item gate so the native
		// mega button is only enabled when they actually brought a Key Stone.
		CobblemonEvents.BATTLE_STARTED_PRE.subscribe(event -> {
			MegaShowdownInjector.injectAll();
			event.getBattle().getPlayers().forEach(MegaEvolution::syncKeyStone);
		});

		// When Showdown mega evolves a Pokémon, mirror the form change on the Minecraft side.
		CobblemonEvents.MEGA_EVOLUTION.subscribe(event ->
			MegaEvolution.applyMega(event.getPokemon().getEffectedPokemon()));

		// Mega Evolution is temporary: revert when the battle ends, is fled, or the Pokémon faints.
		CobblemonEvents.BATTLE_VICTORY.subscribe(event -> MegaEvolution.revertBattle(event.getBattle()));
		CobblemonEvents.BATTLE_FLED.subscribe(event -> MegaEvolution.revertBattle(event.getBattle()));
		CobblemonEvents.BATTLE_FAINTED.subscribe(event -> MegaEvolution.revert(event.getKilled().getEffectedPokemon()));
	}
}
