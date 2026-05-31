package com.aaroncraft.megacobble;

import com.aaroncraft.megacobble.item.ModItems;
import com.aaroncraft.megacobble.mega.MegaEvolution;
import com.aaroncraft.megacobble.net.MegaEvolvePayload;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mega Cobble - a proof-of-concept Cobblemon add-on that introduces Mega Evolution.
 *
 * <p>Common (server + client) entrypoint: registers items, the Mega Evolve packet, and the
 * server-side handler that performs the form change.</p>
 */
public class MegaCobble implements ModInitializer {
	public static final String MOD_ID = "megacobble";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("[Mega Cobble] Initializing Mega Evolution proof of concept for Cobblemon.");

		ModItems.register();

		// Register the client -> server Mega Evolve packet and its server-side handler.
		PayloadTypeRegistry.playC2S().register(MegaEvolvePayload.ID, MegaEvolvePayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(MegaEvolvePayload.ID, (payload, context) -> {
			ServerPlayer player = context.player();
			player.getServer().execute(() -> MegaEvolution.evolve(player, payload.pokemonUuid()));
		});

		// Mega Evolution is temporary: revert when the battle ends, is fled, or the Pokémon faints.
		CobblemonEvents.BATTLE_VICTORY.subscribe(event -> MegaEvolution.revertBattle(event.getBattle()));
		CobblemonEvents.BATTLE_FLED.subscribe(event -> MegaEvolution.revertBattle(event.getBattle()));
		CobblemonEvents.BATTLE_FAINTED.subscribe(event -> MegaEvolution.revert(event.getKilled().getEffectedPokemon()));
	}
}
