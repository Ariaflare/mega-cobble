package com.aaroncraft.megacobble.client;

import com.aaroncraft.megacobble.MegaCobble;
import com.aaroncraft.megacobble.config.MegaCobbleConfig;
import com.aaroncraft.megacobble.item.MegaStones;
import com.aaroncraft.megacobble.mega.MegaEvolution;
import com.aaroncraft.megacobble.net.RequestWorldMegaPayload;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.client.gui.interact.wheel.InteractWheelOption;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.joml.Vector3f;

import java.util.UUID;

/**
 * Client-side entrypoint for Mega Cobble.
 *
 * <p>Adds a "Mega Evolve" option to Cobblemon's shift-right-click interaction wheel (built client
 * side via {@link CobblemonEvents#POKEMON_INTERACTION_GUI_CREATION}). Pressing it asks the server to
 * toggle the Pokémon's out-of-battle ("world") mega; the server is the sole authority on ownership,
 * config, and the Key Stone / Mega Stone gate, and replies with the reason on the action bar.</p>
 *
 * <p>The button is therefore always pressable — it is only <em>tinted</em> grey when the client can
 * tell the Pokémon isn't holding the right stone. The client deliberately does not veto the press:
 * it cannot see the server's config, and {@code Pokemon.heldItem()} is never synced to clients, so
 * any client-side verdict is a guess that would silently dead-end the button on a dedicated server.
 * Vanilla clients without this mod use {@code /megacobble worldmega} instead.</p>
 */
public class MegaCobbleClient implements ClientModInitializer {

	private static final ResourceLocation MEGA_ICON =
		ResourceLocation.fromNamespaceAndPath(MegaCobble.MOD_ID, "textures/gui/interact/mega.png");

	private static final Function0<Vector3f> NO_COLOUR = () -> null;
	private static final Function0<Vector3f> DISABLED_COLOUR = () -> new Vector3f(0.5F, 0.5F, 0.5F);

	@Override
	public void onInitializeClient() {
		CobblemonEvents.POKEMON_INTERACTION_GUI_CREATION.subscribe(event -> {
			UUID pokemonId = event.getPokemonID();
			// Hint only — the server is the authority (it owns the config and the real held item), so
			// the button always sends and always gets a reason back. Gating the press on a client-side
			// guess is what made this a dead button on dedicated servers.
			boolean likelyCanMega = likelyCanMegaEvolve(pokemonId);
			Function0<Unit> onPress = () -> {
				ClientPlayNetworking.send(new RequestWorldMegaPayload(pokemonId));
				Minecraft.getInstance().setScreen(null);
				return Unit.INSTANCE;
			};
			event.addFillingOption(new InteractWheelOption(
				MEGA_ICON, null, true, "megacobble.ui.interact.mega",
				likelyCanMega ? NO_COLOUR : DISABLED_COLOUR, onPress));
		});

		MegaCobble.LOGGER.info("[Mega Cobble] Client initialized.");
	}

	/**
	 * Best-effort client hint for whether the wheel option should look enabled: does this Pokémon have
	 * a mega form, and does it appear to be holding that species' Mega Stone?
	 *
	 * <p>Note {@code Pokemon.heldItem()} is <em>server-only state</em> — it is never sent to clients,
	 * so on a dedicated server it always reads empty. The item the client can actually see is the
	 * entity's synced {@code shownItem}, so that is what we inspect here. This is only a colour hint;
	 * the server re-checks the real held item and reports the outcome.</p>
	 */
	private static boolean likelyCanMegaEvolve(UUID entityId) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) {
			return false;
		}
		PokemonEntity pokemonEntity = null;
		for (Entity entity : mc.level.entitiesForRendering()) {
			if (entity.getUUID().equals(entityId) && entity instanceof PokemonEntity found) {
				pokemonEntity = found;
				break;
			}
		}
		if (pokemonEntity == null) {
			return false;
		}
		Pokemon pokemon = pokemonEntity.getPokemon();
		if (!MegaEvolution.hasMegaForm(pokemon)) {
			return false;
		}
		if (!MegaCobbleConfig.get().requireMegaStone) {
			return true;
		}
		return MegaEvolution.stoneMatches(MegaStones.byCustomData(pokemonEntity.getShownItem()), pokemon);
	}
}
