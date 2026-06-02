package com.aaroncraft.megacobble.net;

import com.aaroncraft.megacobble.MegaCobble;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/**
 * Client -> server request to toggle out-of-battle ("world") Mega Evolution on one of the sender's
 * Pokémon, identified by its entity UUID. Sent by the interaction-wheel "Mega Evolve" button on
 * modded clients; the server validates ownership, config, and the Key Stone / Mega Stone gate.
 */
public record RequestWorldMegaPayload(UUID entityId) implements CustomPacketPayload {

    public static final Type<RequestWorldMegaPayload> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(MegaCobble.MOD_ID, "request_world_mega"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestWorldMegaPayload> CODEC =
        StreamCodec.composite(UUIDUtil.STREAM_CODEC, RequestWorldMegaPayload::entityId, RequestWorldMegaPayload::new);

    @Override
    public Type<RequestWorldMegaPayload> type() {
        return TYPE;
    }
}
