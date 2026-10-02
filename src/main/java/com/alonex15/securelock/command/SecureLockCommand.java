package com.alonex15.securelock.command;

import com.alonex15.securelock.SecureLock;
import com.alonex15.securelock.access.AccessManager;
import com.alonex15.securelock.access.LockDescriber;
import com.alonex15.securelock.access.PermissionHelper;
import com.alonex15.securelock.compat.SecureLockMixinPlugin;
import com.alonex15.securelock.config.SecureLockConfig;
import com.alonex15.securelock.data.LockAdmin;
import com.alonex15.securelock.data.ProtectedBlocksState;
import com.alonex15.securelock.item.AdminToolItem;
import com.alonex15.securelock.lock.LockManager;
import com.alonex15.securelock.network.ServerNetworking;
import com.alonex15.securelock.security.AuditLogger;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * {@code /securelock}: info, trust, untrust, trusted, list, inspect, debug, reload y admin (unlock, transfer, purge,
 * purgeinactive, unlockall, export). Permisos: {@code securelock:command.<nombre>} con fallback a nivel de OP.
 */
public final class SecureLockCommand {
    private static final SuggestionProvider<CommandSourceStack> ONLINE_PLAYERS = (context, builder) ->
            SharedSuggestionProvider.suggest(context.getSource().getServer().getPlayerList().getPlayers().stream()
                    .map(p -> p.getName().getString()), builder);

    private SecureLockCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context, Commands.CommandSelection selection) {
        dispatcher.register(Commands.literal("securelock")
                .then(Commands.literal("info")
                        .requires(PermissionHelper.command("info", PermissionLevel.ALL))
                        .executes(SecureLockCommand::info))
                .then(Commands.literal("trust")
                        .requires(PermissionHelper.command("trust", PermissionLevel.ALL))
                        .then(Commands.argument("player", StringArgumentType.word()).suggests(ONLINE_PLAYERS)
                                .executes(ctx -> trust(ctx, true))))
                .then(Commands.literal("untrust")
                        .requires(PermissionHelper.command("trust", PermissionLevel.ALL))
                        .then(Commands.argument("player", StringArgumentType.word()).suggests(ONLINE_PLAYERS)
                                .executes(ctx -> trust(ctx, false))))
                .then(Commands.literal("trusted")
                        .requires(PermissionHelper.command("trust", PermissionLevel.ALL))
                        .executes(SecureLockCommand::trusted))
                .then(Commands.literal("list")
                        .requires(PermissionHelper.command("list", PermissionLevel.ALL))
                        .executes(SecureLockCommand::list))
                .then(Commands.literal("inspect")
                        .requires(source -> PermissionHelper.has(source, PermissionHelper.ADMIN_INSPECT, PermissionLevel.GAMEMASTERS))
                        .executes(SecureLockCommand::inspect))
                .then(Commands.literal("debug")
                        .requires(PermissionHelper.command("debug", PermissionLevel.GAMEMASTERS))
                        .executes(SecureLockCommand::debug))
                .then(Commands.literal("reload")
                        .requires(PermissionHelper.command("reload", PermissionLevel.GAMEMASTERS))
                        .executes(SecureLockCommand::reload))
                .then(Commands.literal("admin")
                        .requires(PermissionHelper.command("admin", PermissionLevel.GAMEMASTERS))
                        .then(Commands.literal("unlock").executes(SecureLockCommand::adminUnlock))
                        .then(Commands.literal("transfer")
                                .then(Commands.argument("player", StringArgumentType.word()).suggests(ONLINE_PLAYERS)
                                        .executes(SecureLockCommand::adminTransfer)))
                        .then(Commands.literal("purge")
                                .then(Commands.argument("player", StringArgumentType.word()).suggests(ONLINE_PLAYERS)
                                        .executes(SecureLockCommand::adminPurge)))
                        .then(Commands.literal("purgeinactive")
                                .executes(ctx -> adminPurgeInactive(ctx, SecureLockConfig.get().inactiveOwnerDays))
                                .then(Commands.argument("days", IntegerArgumentType.integer(1))
                                        .executes(ctx -> adminPurgeInactive(ctx, IntegerArgumentType.getInteger(ctx, "days")))))
                        .then(Commands.literal("unlockall")
                                .executes(ctx -> {
                                    ctx.getSource().sendFailure(Component.translatable("command.securelock.unlockall_confirm"));
                                    return 0;
                                })
                                .then(Commands.literal("confirm").executes(SecureLockCommand::adminUnlockAll)))
                        .then(Commands.literal("export").executes(SecureLockCommand::adminExport))
                        .then(Commands.literal("rollback")
                                .then(Commands.argument("backup", StringArgumentType.greedyString())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(LockAdmin.backups(), builder))
                                        .executes(SecureLockCommand::adminRollback)))));
    }

    // ---------- Utilidades ----------

    private static LockManager.@Nullable Locked lookedAt(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        HitResult hit = player.pick(player.blockInteractionRange() + 2.0, 1.0F, false);
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
            source.sendFailure(Component.translatable("command.securelock.look_at_block"));
            return null;
        }
        LockManager.Locked locked = LockManager.find(player.level(), blockHit.getBlockPos());
        if (locked == null) {
            source.sendFailure(Component.translatable("message.securelock.not_protected"));
        }
        return locked;
    }

    private static Optional<NameAndId> resolve(CommandSourceStack source, String name) {
        MinecraftServer server = source.getServer();
        Optional<NameAndId> found = ServerNetworking.resolvePlayer(server, name);
        if (found.isEmpty()) {
            found = server.services().nameToIdCache().get(name);
        }
        if (found.isEmpty()) {
            source.sendFailure(Component.translatable("message.securelock.unknown_player", name));
        }
        return found;
    }

    private static String actor(CommandSourceStack source) {
        return source.getTextName();
    }

    // ---------- Jugadores ----------

    private static int info(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        LockManager.Locked locked = lookedAt(ctx.getSource());
        if (locked == null) {
            return 0;
        }
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        boolean admin = PermissionHelper.has(player, PermissionHelper.ADMIN_INSPECT);
        boolean owner = locked.lock().isOwner(player.getUUID());
        if (!owner && !admin) {
            // Los demás solo ven el propietario y el modo, no la lista ni otros detalles
            ctx.getSource().sendSuccess(() -> Component.translatable("info.securelock.short", locked.lock().ownerName(),
                    Component.translatable("mode.securelock." + locked.lock().mode().id())).withStyle(ChatFormatting.GOLD), false);
            return 1;
        }
        LockDescriber.describe(locked, admin, line -> ctx.getSource().sendSuccess(() -> line, false));
        return 1;
    }

    private static int trust(CommandContext<CommandSourceStack> ctx, boolean add) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String name = StringArgumentType.getString(ctx, "player");
        Optional<NameAndId> target = resolve(ctx.getSource(), name);
        if (target.isEmpty()) {
            return 0;
        }
        if (target.get().id().equals(player.getUUID())) {
            ctx.getSource().sendFailure(Component.translatable("command.securelock.trust_self"));
            return 0;
        }
        ProtectedBlocksState state = ProtectedBlocksState.get(ctx.getSource().getServer());
        boolean changed = add ? state.trust(player.getUUID(), target.get().id(), target.get().name()) : state.untrust(player.getUUID(), target.get().id());
        AuditLogger.log(add ? AuditLogger.Event.TRUST : AuditLogger.Event.UNTRUST, player.getName().getString(), "-", target.get().name());
        String key = add ? (changed ? "command.securelock.trusted" : "command.securelock.already_trusted")
                : (changed ? "command.securelock.untrusted" : "command.securelock.not_trusted");
        ctx.getSource().sendSuccess(() -> Component.translatable(key, target.get().name()).withStyle(ChatFormatting.GREEN), false);
        return changed ? 1 : 0;
    }

    private static int trusted(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        Map<UUID, String> trusted = ProtectedBlocksState.get(ctx.getSource().getServer()).trustedBy(player.getUUID());
        ctx.getSource().sendSuccess(() -> Component.translatable("command.securelock.trusted_list",
                trusted.isEmpty() ? "-" : String.join(", ", trusted.values())).withStyle(ChatFormatting.GOLD), false);
        return trusted.size();
    }

    private static int list(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        Set<GlobalPos> blocks = ProtectedBlocksState.get(ctx.getSource().getServer()).blocksOf(player.getUUID());
        int max = SecureLockConfig.get().maxProtectedBlocksPerPlayer;
        ctx.getSource().sendSuccess(() -> Component.translatable("command.securelock.list_header", blocks.size(),
                max > 0 ? String.valueOf(max) : "∞").withStyle(ChatFormatting.GOLD), false);
        blocks.stream().limit(50).forEach(pos -> ctx.getSource().sendSuccess(() -> Component.literal(" - " + pos.dimension().identifier()
                + " " + pos.pos().getX() + " " + pos.pos().getY() + " " + pos.pos().getZ()).withStyle(ChatFormatting.GRAY), false));
        if (blocks.size() > 50) {
            ctx.getSource().sendSuccess(() -> Component.translatable("command.securelock.list_more", blocks.size() - 50), false);
        }
        return blocks.size();
    }

    // ---------- Administración ----------

    private static int inspect(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        LockManager.Locked locked = lookedAt(ctx.getSource());
        if (locked == null) {
            return 0;
        }
        ServerLevel level = ctx.getSource().getPlayerOrException().level();
        LockDescriber.describe(locked, true, line -> ctx.getSource().sendSuccess(() -> line, false));
        List<String> history = AuditLogger.search(AccessManager.describe(level, locked.pos()), 10);
        ctx.getSource().sendSuccess(() -> Component.translatable("command.securelock.history", history.size()).withStyle(ChatFormatting.GOLD), false);
        history.forEach(line -> ctx.getSource().sendSuccess(() -> Component.literal(line).withStyle(ChatFormatting.GRAY), false));
        return 1;
    }

    private static int debug(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        source.sendSuccess(() -> Component.literal("SecureLock debug").withStyle(ChatFormatting.GOLD), false);
        SecureLockMixinPlugin.status().forEach((mixin, ok) -> source.sendSuccess(() -> Component.literal(" " + mixin + ": ")
                .append(Component.literal(ok ? "OK" : "DESACTIVADO").withStyle(ok ? ChatFormatting.GREEN : ChatFormatting.RED)), false));
        List<String> disabled = SecureLockConfig.get().compat.disable;
        source.sendSuccess(() -> Component.literal(" compat.disable: " + (disabled.isEmpty() ? "-" : String.join(", ", disabled))), false);
        ProtectedBlocksState state = ProtectedBlocksState.get(source.getServer());
        source.sendSuccess(() -> Component.literal(" Protecciones indexadas: " + state.totalBlocks() + " (" + state.owners().size() + " propietarios)"), false);
        source.sendSuccess(() -> Component.literal(" Raid window activa: " + AccessManager.isRaidWindowActive()), false);
        return 1;
    }

    private static int reload(CommandContext<CommandSourceStack> ctx) {
        String error = SecureLockConfig.load();
        AccessManager.reconfigure();
        PermissionHelper.invalidate(null);
        if (error != null) {
            ctx.getSource().sendFailure(Component.literal(error));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("command.securelock.reloaded").withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int adminUnlock(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        LockManager.Locked locked = lookedAt(ctx.getSource());
        if (locked == null) {
            return 0;
        }
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        AdminToolItem.forceUnlock(player, player.level(), locked);
        return 1;
    }

    private static int adminTransfer(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        LockManager.Locked locked = lookedAt(ctx.getSource());
        if (locked == null) {
            return 0;
        }
        Optional<NameAndId> target = resolve(ctx.getSource(), StringArgumentType.getString(ctx, "player"));
        if (target.isEmpty()) {
            return 0;
        }
        ServerLevel level = ctx.getSource().getPlayerOrException().level();
        LockAdmin.transfer(level, locked, target.get().id(), target.get().name());
        AuditLogger.log(AuditLogger.Event.ADMIN_TRANSFER, actor(ctx.getSource()), AccessManager.describe(level, locked.pos()),
                locked.lock().ownerName() + " -> " + target.get().name());
        ctx.getSource().sendSuccess(() -> Component.translatable("command.securelock.transferred", target.get().name()).withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int adminPurge(CommandContext<CommandSourceStack> ctx) {
        Optional<NameAndId> target = resolve(ctx.getSource(), StringArgumentType.getString(ctx, "player"));
        if (target.isEmpty()) {
            return 0;
        }
        int removed = LockAdmin.purge(ctx.getSource().getServer(), target.get().id());
        AuditLogger.log(AuditLogger.Event.ADMIN_PURGE, actor(ctx.getSource()), "-", target.get().name() + ", " + removed + " bloques");
        ctx.getSource().sendSuccess(() -> Component.translatable("command.securelock.purged", target.get().name(), removed).withStyle(ChatFormatting.GREEN), true);
        return removed;
    }

    private static int adminPurgeInactive(CommandContext<CommandSourceStack> ctx, int days) {
        if (days <= 0) {
            ctx.getSource().sendFailure(Component.translatable("command.securelock.purge_days"));
            return 0;
        }
        MinecraftServer server = ctx.getSource().getServer();
        ProtectedBlocksState state = ProtectedBlocksState.get(server);
        int total = 0;
        for (UUID owner : LockAdmin.inactiveOwners(server, days)) {
            String name = state.nameOf(owner);
            int removed = LockAdmin.purge(server, owner);
            total += removed;
            AuditLogger.log(AuditLogger.Event.ADMIN_PURGE, actor(ctx.getSource()), "-", name + " inactivo, " + removed + " bloques");
        }
        int finalTotal = total;
        ctx.getSource().sendSuccess(() -> Component.translatable("command.securelock.purged_inactive", days, finalTotal).withStyle(ChatFormatting.GREEN), true);
        return total;
    }

    private static int adminUnlockAll(CommandContext<CommandSourceStack> ctx) {
        int removed = LockAdmin.unlockAll(ctx.getSource().getServer());
        AuditLogger.log(AuditLogger.Event.ADMIN_UNLOCK_ALL, actor(ctx.getSource()), "-", removed + " bloques");
        ctx.getSource().sendSuccess(() -> Component.translatable("command.securelock.unlocked_all", removed).withStyle(ChatFormatting.GREEN), true);
        return removed;
    }

    private static int adminRollback(CommandContext<CommandSourceStack> ctx) {
        String backup = StringArgumentType.getString(ctx, "backup").trim();
        try {
            LockAdmin.RollbackResult result = LockAdmin.rollback(ctx.getSource().getServer(), backup);
            AuditLogger.log(AuditLogger.Event.ADMIN_ROLLBACK, actor(ctx.getSource()), "-", backup + ": " + result.restored() + " restaurados, " + result.skipped() + " omitidos");
            ctx.getSource().sendSuccess(() -> Component.translatable("command.securelock.rolled_back", result.restored(), result.skipped())
                    .withStyle(ChatFormatting.GREEN), true);
            return result.restored();
        } catch (IOException | RuntimeException e) {
            ctx.getSource().sendFailure(Component.literal(e.getMessage() == null ? e.toString() : e.getMessage()));
            return 0;
        }
    }

    private static int adminExport(CommandContext<CommandSourceStack> ctx) {
        try {
            Path file = LockAdmin.export(ctx.getSource().getServer());
            ctx.getSource().sendSuccess(() -> Component.translatable("command.securelock.exported", file.toString()).withStyle(ChatFormatting.GREEN), true);
            return 1;
        } catch (IOException e) {
            SecureLock.LOGGER.warn("No se pudo exportar", e);
            ctx.getSource().sendFailure(Component.literal(e.getMessage()));
            return 0;
        }
    }
}
