package com.aaroncraft.megacobble.command;

import com.aaroncraft.megacobble.battle.BattleDamageMonitor;
import com.aaroncraft.megacobble.config.MegaCobbleConfig;
import com.aaroncraft.megacobble.item.MegaItems;
import com.aaroncraft.megacobble.item.MegaStones;
import com.aaroncraft.megacobble.item.ZCrystals;
import com.aaroncraft.megacobble.mega.MegaEvolution;
import com.aaroncraft.megacobble.mega.MegaEvolution.WorldMegaResult;
import com.aaroncraft.megacobble.skin.GlobalSkins;
import com.aaroncraft.megacobble.variant.MegaVariants;
import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * The {@code /megacobble} command tree. Player-facing {@code worldmega} sub-command to mega-evolve a
 * Pokémon for overworld exploration (the universal equivalent of the modded-client interaction-wheel
 * button), plus an op-gated {@code config} sub-command to read/edit the live config.
 */
public final class MegaCobbleCommands {

    private MegaCobbleCommands() {}

    private static final double LOOK_REACH = 6.0;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("megacobble")
            .then(Commands.literal("worldmega")
                .executes(ctx -> worldMega(ctx.getSource(), Mode.TOGGLE, null))
                .then(Commands.literal("on").executes(ctx -> worldMega(ctx.getSource(), Mode.ON, null)))
                .then(Commands.literal("off").executes(ctx -> worldMega(ctx.getSource(), Mode.OFF, null)))
                .then(Commands.literal("toggle").executes(ctx -> worldMega(ctx.getSource(), Mode.TOGGLE, null)))
                .then(Commands.literal("slot")
                    .then(Commands.argument("slot", IntegerArgumentType.integer(1, 6))
                        .executes(ctx -> worldMega(ctx.getSource(), Mode.TOGGLE, IntegerArgumentType.getInteger(ctx, "slot")))
                        .then(Commands.literal("on").executes(ctx -> worldMega(ctx.getSource(), Mode.ON, IntegerArgumentType.getInteger(ctx, "slot"))))
                        .then(Commands.literal("off").executes(ctx -> worldMega(ctx.getSource(), Mode.OFF, IntegerArgumentType.getInteger(ctx, "slot"))))
                        .then(Commands.literal("toggle").executes(ctx -> worldMega(ctx.getSource(), Mode.TOGGLE, IntegerArgumentType.getInteger(ctx, "slot")))))))
            // Per-player battle-damage readout. Available to everyone (no permission gate).
            .then(Commands.literal("damage")
                .executes(ctx -> damageDisplay(ctx.getSource(), Mode.TOGGLE))
                .then(Commands.literal("on").executes(ctx -> damageDisplay(ctx.getSource(), Mode.ON)))
                .then(Commands.literal("off").executes(ctx -> damageDisplay(ctx.getSource(), Mode.OFF)))
                .then(Commands.literal("toggle").executes(ctx -> damageDisplay(ctx.getSource(), Mode.TOGGLE))))
            .then(Commands.literal("give")
                .requires(src -> src.hasPermission(2))
                .then(Commands.argument("stone", StringArgumentType.word())
                    .suggests((c, b) -> SharedSuggestionProvider.suggest(giveSuggestions(), b))
                    .executes(ctx -> give(ctx.getSource(), StringArgumentType.getString(ctx, "stone"), 1, null))
                    .then(Commands.argument("count", IntegerArgumentType.integer(1, 64))
                        .executes(ctx -> give(ctx.getSource(), StringArgumentType.getString(ctx, "stone"),
                            IntegerArgumentType.getInteger(ctx, "count"), null))
                        .then(Commands.argument("targets", EntityArgument.players())
                            .executes(ctx -> give(ctx.getSource(), StringArgumentType.getString(ctx, "stone"),
                                IntegerArgumentType.getInteger(ctx, "count"), EntityArgument.getPlayers(ctx, "targets")))))
                    .then(Commands.argument("targets", EntityArgument.players())
                        .executes(ctx -> give(ctx.getSource(), StringArgumentType.getString(ctx, "stone"), 1,
                            EntityArgument.getPlayers(ctx, "targets"))))))
            .then(Commands.literal("variant")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("list").executes(ctx -> listVariants(ctx.getSource())))
                .then(Commands.literal("reload").executes(ctx -> reloadVariants(ctx.getSource())))
                .then(variantOpNode("apply", true))
                .then(variantOpNode("remove", false)))
            .then(Commands.literal("skin")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("set")
                    .then(Commands.argument("aspect", StringArgumentType.word())
                        .executes(ctx -> skinSet(ctx.getSource(), StringArgumentType.getString(ctx, "aspect"), null))
                        .then(Commands.literal("all")
                            .executes(ctx -> skinSetGlobal(ctx.getSource(), StringArgumentType.getString(ctx, "aspect"))))
                        .then(Commands.literal("slot").then(Commands.argument("slot", IntegerArgumentType.integer(1, 6))
                            .executes(ctx -> skinSet(ctx.getSource(), StringArgumentType.getString(ctx, "aspect"),
                                IntegerArgumentType.getInteger(ctx, "slot")))))))
                .then(Commands.literal("clear")
                    .executes(ctx -> skinClear(ctx.getSource(), null))
                    .then(Commands.literal("all")
                        .executes(ctx -> skinClearGlobal(ctx.getSource())))
                    .then(Commands.literal("slot").then(Commands.argument("slot", IntegerArgumentType.integer(1, 6))
                        .executes(ctx -> skinClear(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "slot")))))))
            .then(Commands.literal("config")
                .requires(src -> src.hasPermission(2))
                .executes(ctx -> listConfig(ctx.getSource()))
                .then(Commands.argument("key", StringArgumentType.word())
                    .suggests((c, b) -> net.minecraft.commands.SharedSuggestionProvider.suggest(MegaCobbleConfig.KEYS, b))
                    .then(Commands.argument("value", BoolArgumentType.bool())
                        .executes(ctx -> setConfig(ctx.getSource(),
                            StringArgumentType.getString(ctx, "key"),
                            BoolArgumentType.getBool(ctx, "value")))))));
    }

    private enum Mode { ON, OFF, TOGGLE }

    /** Suggestions for {@code /megacobble give}: "random", the Key Stone, plus every Mega Stone id. */
    private static Iterable<String> giveSuggestions() {
        List<String> ids = new ArrayList<>();
        ids.add("random");
        ids.add(MegaItems.KEY_STONE_ID);
        MegaStones.stoneIds().forEach(ids::add);
        ids.add(MegaItems.Z_RING_ID);
        ZCrystals.crystalIds().forEach(ids::add);
        return ids;
    }

    /**
     * Gives a Mega Stone / Key Stone to the recipients. {@code targets} null = the executing player,
     * otherwise the resolved player selector. {@code id} "random" gives each recipient an independently
     * random Mega Stone. Op-gated by the command tree.
     */
    private static int give(CommandSourceStack source, String id, int count, Collection<ServerPlayer> targets) {
        Collection<ServerPlayer> recipients;
        if (targets != null) {
            recipients = targets;
        } else {
            ServerPlayer self = source.getPlayer();
            if (self == null) {
                source.sendFailure(Component.translatable("megacobble.command.player_only"));
                return 0;
            }
            recipients = List.of(self);
        }

        boolean random = "random".equalsIgnoreCase(id);
        boolean keyStone = MegaItems.KEY_STONE_ID.equalsIgnoreCase(id);
        boolean zRing = MegaItems.Z_RING_ID.equalsIgnoreCase(id);
        MegaStones.MegaStone fixed = null;
        ZCrystals.ZCrystal fixedCrystal = null;
        if (!random && !keyStone && !zRing) {
            fixed = MegaStones.byStoneId(id);
            if (fixed == null) {
                fixedCrystal = ZCrystals.byCrystalId(id);
            }
            if (fixed == null && fixedCrystal == null) {
                source.sendFailure(Component.translatable("megacobble.command.give.unknown", id));
                return 0;
            }
        }
        List<MegaStones.MegaStone> pool = MegaStones.all();

        for (ServerPlayer player : recipients) {
            ItemStack stack;
            if (keyStone) {
                stack = MegaItems.createKeyStone();
            } else if (zRing) {
                stack = MegaItems.createZRing();
            } else if (fixedCrystal != null) {
                stack = MegaItems.createZCrystal(fixedCrystal);
            } else {
                MegaStones.MegaStone stone = random ? pool.get(ThreadLocalRandom.current().nextInt(pool.size())) : fixed;
                stack = MegaItems.createStone(stone);
            }
            stack.setCount(count);
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
        }

        String what = random ? "a random Mega Stone"
            : keyStone ? "Key Stone"
            : zRing ? "Z-Ring"
            : fixedCrystal != null ? fixedCrystal.name()
            : fixed.name();
        if (targets == null) {
            source.sendSuccess(() -> Component.translatable("megacobble.command.give.given", count, what)
                .withStyle(ChatFormatting.GREEN), false);
        } else {
            String who = recipients.size() == 1
                ? recipients.iterator().next().getGameProfile().getName()
                : recipients.size() + " players";
            source.sendSuccess(() -> Component.translatable("megacobble.command.give.given_to", count, what, who)
                .withStyle(ChatFormatting.GREEN), true);
        }
        return 1;
    }

    /** Builds an {@code apply|remove <variant> [slot <n>]} subtree for the variant command. */
    private static LiteralArgumentBuilder<CommandSourceStack> variantOpNode(String literal, boolean apply) {
        return Commands.literal(literal)
            .then(Commands.argument("variant", StringArgumentType.word())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(MegaVariants.ids(), b))
                .executes(ctx -> variant(ctx.getSource(), StringArgumentType.getString(ctx, "variant"), apply, null))
                .then(Commands.literal("slot").then(Commands.argument("slot", IntegerArgumentType.integer(1, 6))
                    .executes(ctx -> variant(ctx.getSource(), StringArgumentType.getString(ctx, "variant"), apply,
                        IntegerArgumentType.getInteger(ctx, "slot"))))));
    }

    /** Applies or removes a named visual variant (its aspect set) on the targeted Pokémon. */
    private static int variant(CommandSourceStack source, String id, boolean apply, Integer slot) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("megacobble.command.player_only"));
            return 0;
        }
        MegaVariants.Variant variant = MegaVariants.byId(id);
        if (variant == null) {
            source.sendFailure(Component.translatable("megacobble.command.variant.unknown", id));
            return 0;
        }
        Pokemon target = slot != null ? partySlot(player, slot) : lookedAtOwned(player);
        if (target == null) {
            source.sendFailure(Component.translatable(slot != null
                ? "megacobble.command.worldmega.empty_slot"
                : "megacobble.command.worldmega.no_target"));
            return 0;
        }
        MegaEvolution.setVariant(target, variant.aspects(), apply);
        source.sendSuccess(() -> Component.translatable(
            apply ? "megacobble.command.variant.applied" : "megacobble.command.variant.removed", variant.label())
            .withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int listVariants(CommandSourceStack source) {
        source.sendSuccess(() -> Component.translatable("megacobble.command.variant.header")
            .withStyle(ChatFormatting.GOLD), false);
        for (MegaVariants.Variant variant : MegaVariants.all()) {
            String scope = variant.species() != null ? variant.species() : "any";
            source.sendSuccess(() -> Component.literal("  " + variant.id() + " ")
                .append(Component.literal("(" + variant.kind() + ", " + scope + ")").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(" — " + variant.label())), false);
        }
        return 1;
    }

    /**
     * Forces an arbitrary skin aspect onto the targeted Pokémon. Skin-agnostic: the aspect can be one
     * an installed datapack/resource pack defines — its resolver renders it. The default (e.g. the mega
     * substitute doll) and any datapack skin are overridden while this forced aspect is set.
     */
    private static int skinSet(CommandSourceStack source, String aspect, Integer slot) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("megacobble.command.player_only"));
            return 0;
        }
        Pokemon target = slot != null ? partySlot(player, slot) : lookedAtOwned(player);
        if (target == null) {
            source.sendFailure(Component.translatable(slot != null
                ? "megacobble.command.worldmega.empty_slot"
                : "megacobble.command.worldmega.no_target"));
            return 0;
        }
        MegaEvolution.setVariant(target, List.of(aspect), true);
        source.sendSuccess(() -> Component.translatable("megacobble.command.skin.set", aspect)
            .withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    /** Clears forced aspects, releasing the Pokémon to its default look / datapack-driven skin. */
    private static int skinClear(CommandSourceStack source, Integer slot) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("megacobble.command.player_only"));
            return 0;
        }
        Pokemon target = slot != null ? partySlot(player, slot) : lookedAtOwned(player);
        if (target == null) {
            source.sendFailure(Component.translatable(slot != null
                ? "megacobble.command.worldmega.empty_slot"
                : "megacobble.command.worldmega.no_target"));
            return 0;
        }
        MegaEvolution.clearForcedAspects(target);
        source.sendSuccess(() -> Component.translatable("megacobble.command.skin.cleared")
            .withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    /**
     * Sets a server-wide skin for the species of the Pokémon you're looking at: an aspect provider
     * applies it to <em>every</em> Pokémon of that species (loaded, boxed, or caught later). The
     * species is identified from the looked-at Pokémon (any owner).
     */
    private static int skinSetGlobal(CommandSourceStack source, String aspect) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("megacobble.command.player_only"));
            return 0;
        }
        PokemonEntity looked = lookedAtPokemonEntity(player);
        if (looked == null) {
            source.sendFailure(Component.translatable("megacobble.command.skin.no_species"));
            return 0;
        }
        String species = looked.getPokemon().getSpecies().getResourceIdentifier().getPath();
        GlobalSkins.set(species, aspect);
        int refreshed = refreshSpecies(source.getServer(), species);
        source.sendSuccess(() -> Component.translatable("megacobble.command.skin.set_global", aspect, species, refreshed)
            .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    /** Clears the server-wide skin for the species of the Pokémon you're looking at. */
    private static int skinClearGlobal(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("megacobble.command.player_only"));
            return 0;
        }
        PokemonEntity looked = lookedAtPokemonEntity(player);
        if (looked == null) {
            source.sendFailure(Component.translatable("megacobble.command.skin.no_species"));
            return 0;
        }
        String species = looked.getPokemon().getSpecies().getResourceIdentifier().getPath();
        GlobalSkins.clear(species);
        int refreshed = refreshSpecies(source.getServer(), species);
        source.sendSuccess(() -> Component.translatable("megacobble.command.skin.cleared_global", species, refreshed)
            .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    /** @return the Pokémon entity the player is looking at (any owner), used to pick a species. */
    private static PokemonEntity lookedAtPokemonEntity(ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 reach = player.getViewVector(1.0F).scale(LOOK_REACH);
        Vec3 end = eye.add(reach);
        AABB box = player.getBoundingBox().expandTowards(reach).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
            player, eye, end, box, e -> e instanceof PokemonEntity, LOOK_REACH * LOOK_REACH);
        return (hit != null && hit.getEntity() instanceof PokemonEntity pe) ? pe : null;
    }

    /** Recomputes aspects on every loaded Pokémon of the species so the global skin shows at once. */
    private static int refreshSpecies(MinecraftServer server, String speciesPath) {
        int count = 0;
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity e : level.getAllEntities()) {
                if (e instanceof PokemonEntity pe
                    && pe.getPokemon().getSpecies().getResourceIdentifier().getPath().equals(speciesPath)) {
                    pe.getPokemon().updateAspects();
                    count++;
                }
            }
        }
        return count;
    }

    private static int reloadVariants(CommandSourceStack source) {
        MegaVariants.load();
        source.sendSuccess(() -> Component.translatable("megacobble.command.variant.reloaded",
            MegaVariants.all().size()).withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int worldMega(CommandSourceStack source, Mode mode, Integer slot) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("megacobble.command.player_only"));
            return 0;
        }
        Pokemon target = slot != null ? partySlot(player, slot) : lookedAtOwned(player);
        if (target == null) {
            source.sendFailure(Component.translatable(slot != null
                ? "megacobble.command.worldmega.empty_slot"
                : "megacobble.command.worldmega.no_target"));
            return 0;
        }

        WorldMegaResult result;
        boolean isMega = MegaEvolution.isWorldMega(target);
        switch (mode) {
            case ON -> result = isMega ? WorldMegaResult.ALREADY_MEGA : MegaEvolution.applyWorldMega(player, target);
            case OFF -> result = MegaEvolution.revertWorldMega(target) ? WorldMegaResult.REVERTED : WorldMegaResult.NOT_MEGA;
            default -> result = MegaEvolution.toggleWorldMega(player, target);
        }
        sendResult(source, result);
        return isSuccess(result) ? 1 : 0;
    }

    /** @return the player's owned Pokémon entity they're looking at, or null. */
    private static Pokemon lookedAtOwned(ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        Vec3 reach = player.getViewVector(1.0F).scale(LOOK_REACH);
        Vec3 end = eye.add(reach);
        AABB box = player.getBoundingBox().expandTowards(reach).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
            player, eye, end, box,
            e -> e instanceof PokemonEntity, LOOK_REACH * LOOK_REACH);
        if (hit != null && hit.getEntity() instanceof PokemonEntity pe) {
            Pokemon pokemon = pe.getPokemon();
            if (player.getUUID().equals(pokemon.getOwnerUUID())) {
                return pokemon;
            }
        }
        return null;
    }

    /** @return the Pokémon in the player's party slot (1-6), or null if empty. */
    private static Pokemon partySlot(ServerPlayer player, int slot) {
        return Cobblemon.INSTANCE.getStorage().getParty(player).get(slot - 1);
    }

    private static int listConfig(CommandSourceStack source) {
        MegaCobbleConfig cfg = MegaCobbleConfig.get();
        source.sendSuccess(() -> Component.translatable("megacobble.command.config.header")
            .withStyle(ChatFormatting.GOLD), false);
        for (String key : MegaCobbleConfig.KEYS) {
            boolean value = Boolean.TRUE.equals(cfg.getBool(key));
            source.sendSuccess(() -> Component.literal("  " + key + ": ")
                .append(Component.literal(String.valueOf(value))
                    .withStyle(value ? ChatFormatting.GREEN : ChatFormatting.RED)), false);
        }
        return 1;
    }

    /** Per-player toggle for the in-battle damage readout. Available to any player. */
    private static int damageDisplay(CommandSourceStack source, Mode mode) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("megacobble.command.player_only"));
            return 0;
        }
        boolean enabled = switch (mode) {
            case ON -> BattleDamageMonitor.setEnabled(player.getUUID(), true);
            case OFF -> BattleDamageMonitor.setEnabled(player.getUUID(), false);
            case TOGGLE -> BattleDamageMonitor.toggle(player.getUUID());
        };
        source.sendSuccess(() -> Component.translatable(
                enabled ? "megacobble.command.damage.on" : "megacobble.command.damage.off")
            .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.YELLOW), false);
        return 1;
    }

    private static int setConfig(CommandSourceStack source, String key, boolean value) {
        if (!MegaCobbleConfig.get().setBool(key, value)) {
            source.sendFailure(Component.translatable("megacobble.command.config.unknown_key", key));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("megacobble.command.config.set", key, value)
            .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static boolean isSuccess(WorldMegaResult result) {
        return result == WorldMegaResult.APPLIED || result == WorldMegaResult.REVERTED;
    }

    private static void sendResult(CommandSourceStack source, WorldMegaResult result) {
        String key = "megacobble.feedback." + result.name().toLowerCase(java.util.Locale.ROOT);
        if (isSuccess(result)) {
            source.sendSuccess(() -> Component.translatable(key).withStyle(ChatFormatting.GREEN), false);
        } else {
            source.sendFailure(Component.translatable(key));
        }
    }
}
