package com.aaroncraft.megacobble.item;

import com.aaroncraft.megacobble.item.MegaStones.MegaStone;
import com.aaroncraft.megacobble.item.ZCrystals.ZCrystal;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;

/**
 * Mega Cobble's "items" are plain vanilla items carrying a {@code minecraft:custom_data} tag that
 * identifies them server-side ({@code {megacobble:{id:"venusaurite"}}}).
 *
 * <p>Using vanilla items instead of registered custom items means a player on a server needs no
 * client mod: every client already knows the base item, the server reads the stone identity from the
 * tag, and a resource pack can re-skin the base item per {@code custom_model_data} (added with the
 * pack in a later step). Until then the stones render as named base items.</p>
 */
public final class MegaItems {

    private MegaItems() {}

    /** The vanilla item every Mega Stone / Key Stone is built on. */
    public static final Item BASE_ITEM = Items.AMETHYST_SHARD;

    /** custom_data layout: {@code {megacobble:{id:"<id>"}}}. */
    private static final String ROOT = "megacobble";
    private static final String ID = "id";

    /** Reserved id for the Key Stone (which isn't a Mega Stone manifest entry). */
    public static final String KEY_STONE_ID = "key_stone";

    /** Reserved id for the Z-Ring (the Z-Move trainer item; not a Z-Crystal manifest entry). */
    public static final String Z_RING_ID = "z_ring";

    /** custom_model_data reserved for the Key Stone; manifest stones use {@code MegaStone.customModelData()}. */
    public static final int KEY_STONE_CMD = 1;

    /** custom_model_data reserved for the Z-Ring. Z items live in a separate range from Mega Stones. */
    public static final int Z_RING_CMD = 100;

    /** First custom_model_data for a manifest Z-Crystal ({@link ZCrystals} assigns upward from here). */
    public static final int Z_CRYSTAL_CMD_START = 101;

    public static ItemStack createStone(MegaStone stone) {
        return create(stone.stoneId(), stone.name(), stone.customModelData());
    }

    public static ItemStack createKeyStone() {
        return create(KEY_STONE_ID, "Key Stone", KEY_STONE_CMD);
    }

    public static ItemStack createZCrystal(ZCrystal crystal) {
        return create(crystal.crystalId(), crystal.name(), crystal.customModelData());
    }

    public static ItemStack createZRing() {
        return create(Z_RING_ID, "Z-Ring", Z_RING_CMD);
    }

    /**
     * Builds the tagged item: vanilla base item + custom_data identity + a display name, plus a
     * custom_model_data hook so an external resource pack can re-skin it. The mod ships no stone
     * texture itself — without a pack the stone renders as the plain base item.
     */
    private static ItemStack create(String id, String displayName, int customModelData) {
        ItemStack stack = new ItemStack(BASE_ITEM);
        CompoundTag root = new CompoundTag();
        CompoundTag mega = new CompoundTag();
        mega.putString(ID, id);
        root.put(ROOT, mega);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        stack.set(DataComponents.ITEM_NAME, Component.literal(displayName));
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(customModelData));
        return stack;
    }

    /** @return the megacobble id in the stack's custom_data, or null if it isn't one of ours. */
    public static String idOf(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return null;
        }
        CompoundTag tag = data.copyTag();
        if (!tag.contains(ROOT)) {
            return null;
        }
        CompoundTag mega = tag.getCompound(ROOT);
        return mega.contains(ID) ? mega.getString(ID) : null;
    }

    /** @return true if the stack is a Key Stone (by custom_data). */
    public static boolean isKeyStone(ItemStack stack) {
        return KEY_STONE_ID.equals(idOf(stack));
    }

    /** @return true if the stack is a Z-Ring (by custom_data). */
    public static boolean isZRing(ItemStack stack) {
        return Z_RING_ID.equals(idOf(stack));
    }
}
