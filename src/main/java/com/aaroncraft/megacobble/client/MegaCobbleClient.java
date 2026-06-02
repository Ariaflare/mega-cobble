package com.aaroncraft.megacobble.client;

import com.aaroncraft.megacobble.MegaCobble;
import com.aaroncraft.megacobble.config.MegaCobbleConfig;
import com.aaroncraft.megacobble.item.MegaStones;
import com.aaroncraft.megacobble.net.RequestWorldMegaPayload;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.client.gui.interact.wheel.InteractWheelOption;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.FormData;
import com.cobblemon.mod.common.pokemon.Pokemon;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.joml.Vector3f;

import java.util.Locale;
import java.util.UUID;

/**
 * Client-side entrypoint for Mega Cobble.
 *
 * <p>Adds a "Mega Evolve" option to Cobblemon's shift-right-click interaction wheel (built client
 * side via {@link CobblemonEvents#POKEMON_INTERACTION_GUI_CREATION}). Pressing it asks the server to
 * toggle the Pokémon's out-of-battle ("world") mega; the server enforces ownership, config, and the
 * Key Stone / Mega Stone gate. The option is greyed out when the Pokémon isn't holding the right Mega
 * Stone, and hidden entirely when {@code allowRideInMega} is off. Vanilla clients without this mod
 * don't get the button and use the {@code /megacobble worldmega} command instead.</p>
 */
public class MegaCobbleClient implements ClientModInitializer {

	private static final ResourceLocation MEGA_ICON =
		ResourceLocation.fromNamespaceAndPath(MegaCobble.MOD_ID, "textures/gui/interact/mega.png");

	private static final Function0<Vector3f> NO_COLOUR = () -> null;
	private static final Function0<Vector3f> DISABLED_COLOUR = () -> new Vector3f(0.5F, 0.5F, 0.5F);

	@Override
	public void onInitializeClient() {
		CobblemonEvents.POKEMON_INTERACTION_GUI_CREATION.subscribe(event -> {
			if (!MegaCobbleConfig.get().allowRideInMega) {
				return;
			}
			UUID pokemonId = event.getPokemonID();
			boolean canMega = canMegaEvolve(pokemonId);
			Function0<Unit> onPress = () -> {
				if (canMega) {
					ClientPlayNetworking.send(new RequestWorldMegaPayload(pokemonId));
					Minecraft.getInstance().setScreen(null);
				}
				return Unit.INSTANCE;
			};
			event.addFillingOption(new InteractWheelOption(
				MEGA_ICON, null, canMega, "megacobble.ui.interact.mega",
				canMega ? NO_COLOUR : DISABLED_COLOUR, onPress));
		});

		MegaCobble.LOGGER.info("[Mega Cobble] Client initialized.");
	}

	/**
	 * Client-side check used to enable/disable the wheel option: can this Pokémon mega-evolve right
	 * now? By default that means it's holding its matching Mega Stone (mirrors the server gate); if
	 * the Mega Stone requirement is off, any Pokémon with a mega form qualifies.
	 */
	private static boolean canMegaEvolve(UUID entityId) {
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
		if (!MegaCobbleConfig.get().requireMegaStone) {
			return hasMegaForm(pokemon);
		}
		MegaStones.MegaStone stone = MegaStones.byCustomData(pokemon.heldItem());
		return stone != null
			&& stone.species().equalsIgnoreCase(pokemon.getSpecies().getResourceIdentifier().getPath());
	}

	private static boolean hasMegaForm(Pokemon pokemon) {
		for (FormData form : pokemon.getSpecies().getForms()) {
			if (form.getName().toLowerCase(Locale.ROOT).startsWith("mega")) {
				return true;
			}
		}
		return false;
	}
}
