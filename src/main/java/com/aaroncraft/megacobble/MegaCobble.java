package com.aaroncraft.megacobble;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mega Cobble - a proof-of-concept Cobblemon add-on that introduces Mega Evolution.
 *
 * <p>This is the common (server + client) entrypoint. For the proof of concept it only
 * confirms that the mod loads alongside Cobblemon; the Mega Evolution mechanics will be
 * built out from here.</p>
 */
public class MegaCobble implements ModInitializer {
	public static final String MOD_ID = "megacobble";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("[Mega Cobble] Initializing Mega Evolution proof of concept for Cobblemon.");
	}
}
