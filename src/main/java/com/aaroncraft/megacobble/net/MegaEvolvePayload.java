package com.aaroncraft.megacobble.net;

import com.aaroncraft.megacobble.MegaCobble;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/**
 * Client -> Server request to Mega Evolve a specific Pokémon (identified by its UUID).
 * The server validates (held Mega Stone + Key Stone in inventory) before applying the form change.
 */
public record MegaEvolvePayload(UUID pokemonUuid) implements CustomPacketPayload {

    public static final Type<MegaEvolvePayload> ID =
        new Type<>(ResourceLocation.fromNamespaceAndPath(MegaCobble.MOD_ID, "mega_evolve"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MegaEvolvePayload> CODEC =
        StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, MegaEvolvePayload::pokemonUuid,
            MegaEvolvePayload::new
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
