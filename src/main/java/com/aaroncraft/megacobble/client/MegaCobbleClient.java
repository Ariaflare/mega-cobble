package com.aaroncraft.megacobble.client;

import com.aaroncraft.megacobble.MegaCobble;
import net.fabricmc.api.ClientModInitializer;

/**
 * Client-side entrypoint for Mega Cobble.
 */
public class MegaCobbleClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		MegaCobble.LOGGER.info("[Mega Cobble] Client initialized.");
	}
}
