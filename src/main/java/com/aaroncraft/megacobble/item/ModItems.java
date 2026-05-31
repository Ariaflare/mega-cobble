package com.aaroncraft.megacobble.item;

import com.aaroncraft.megacobble.MegaCobble;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Items added by Mega Cobble.
 *
 * <ul>
 *   <li>{@link #KEY_STONE} — the trainer's Key Stone. Held in the player's inventory; required to Mega Evolve.</li>
 *   <li>All Mega Stones — registered from the manifest by {@link MegaStones}. Each is given to a Pokémon as its held item.</li>
 * </ul>
 * Everything is grouped into a dedicated "Mega Stones" creative tab.
 */
public final class ModItems {

    public static final Item KEY_STONE = new Item(new Item.Properties().stacksTo(1));

    private ModItems() {}

    public static void register() {
        Registry.register(BuiltInRegistries.ITEM,
            ResourceLocation.fromNamespaceAndPath(MegaCobble.MOD_ID, "key_stone"), KEY_STONE);

        MegaStones.registerAll();

        // Dedicated creative tab holding the Key Stone and every Mega Stone.
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,
            ResourceLocation.fromNamespaceAndPath(MegaCobble.MOD_ID, "mega_stones"),
            FabricItemGroup.builder()
                .title(Component.translatable("itemGroup.megacobble.mega_stones"))
                .icon(() -> new ItemStack(KEY_STONE))
                .displayItems((params, output) -> {
                    output.accept(KEY_STONE);
                    for (MegaStones.MegaStone stone : MegaStones.all()) {
                        output.accept(stone.item());
                    }
                })
                .build());
    }
}
