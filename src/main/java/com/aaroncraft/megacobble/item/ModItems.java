package com.aaroncraft.megacobble.item;

import com.aaroncraft.megacobble.MegaCobble;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

/**
 * Items added by Mega Cobble.
 *
 * <ul>
 *   <li>{@link #KEY_STONE} — the trainer's Key Stone. Held in the player's inventory; required to Mega Evolve.</li>
 *   <li>{@link #VENUSAURITE} — Venusaur's Mega Stone. Given to a Pokémon as its held item.</li>
 * </ul>
 */
public final class ModItems {

    public static final Item KEY_STONE = new Item(new Item.Properties().stacksTo(1));
    public static final Item VENUSAURITE = new Item(new Item.Properties().stacksTo(1));

    private ModItems() {}

    public static void register() {
        registerItem("key_stone", KEY_STONE);
        registerItem("venusaurite", VENUSAURITE);

        // Make them easy to grab while testing.
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.INGREDIENTS).register(entries -> {
            entries.accept(KEY_STONE);
            entries.accept(VENUSAURITE);
        });
    }

    private static void registerItem(String name, Item item) {
        Registry.register(
            BuiltInRegistries.ITEM,
            ResourceLocation.fromNamespaceAndPath(MegaCobble.MOD_ID, name),
            item
        );
    }
}
