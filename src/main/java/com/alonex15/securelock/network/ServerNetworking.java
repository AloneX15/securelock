package com.alonex15.securelock.network;

import com.alonex15.securelock.SecureLock;
import com.alonex15.securelock.access.AccessManager;
import com.alonex15.securelock.block.CardReaderBlock;
import com.alonex15.securelock.block.KeypadBlock;
import com.alonex15.securelock.block.SecureEmitterBlock;
import com.alonex15.securelock.config.SecureLockConfig;
import com.alonex15.securelock.data.ProtectedBlocksState;
import com.alonex15.securelock.lock.LockData;
import com.alonex15.securelock.lock.LockKind;
import com.alonex15.securelock.lock.LockManager;
import com.alonex15.securelock.security.AccessMode;
import com.alonex15.securelock.security.AuditLogger;
import com.alonex15.securelock.security.PasscodeHasher;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/** Manejadores de los paquetes en el servidor. Cada uno valida todo y nunca lanza excepciones. */
public final class ServerNetworking {
    private static final Pattern PLAYER_NAME = Pattern.compile("[A-Za-z0-9_]{1,16}");

    private ServerNetworking() {
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(Payloads.SubmitCodeC2S.TYPE, (payload, context) ->
                guarded(context.player(), "submit_code", () -> onSubmitCode(context.player(), payload)));
        ServerPlayNetworking.registerGlobalReceiver(Payloads.SetCodeC2S.TYPE, (payload, context) ->
                guarded(context.player(), "set_code", () -> onSetCode(context.player(), payload)));
        ServerPlayNetworking.registerGlobalReceiver(Payloads.UpdateAllowListC2S.TYPE, (payload, context) ->
                guarded(context.player(), "allow_list", () -> onUpdateAllowList(context.player(), payload)));
        ServerPlayNetworking.registerGlobalReceiver(Payloads.SetModeC2S.TYPE, (payload, context) ->
                guarded(context.player(), "set_mode", () -> onSetMode(context.player(), payload)));
    }

    /** Rate limiting + try/catch: un paquete malicioso nunca tumba el servidor. */
    private static void guarded(ServerPlayer player, String name, Runnable handler) {
        if (!AccessManager.rateLimiter().tryAcquire(player.getUUID())) {
            return;
        }
        try {
            handler.run();
        } catch (RuntimeException e) {
            SecureLock.warnOnce("packet_" + name, "Error procesando el paquete " + name, e);
        }
    }

    /** Valida dimensión, chunk cargado, distancia y que el bloque esté protegido. */
    private static LockManager.@Nullable Locked validate(ServerPlayer player, BlockPos pos, String packet) {
        ServerLevel level = player.level();
        if (LockManager.loadedChunk(level, pos) == null || !AccessManager.isWithinReach(player, pos)) {
            AuditLogger.log(AuditLogger.Event.PACKET_REJECTED, player.getName().getString(), AccessManager.describe(level, pos), packet + " fuera de alcance");
            return null;
        }
        return LockManager.find(level, pos);
    }

    // ---------- Introducir código ----------

    private static void onSubmitCode(ServerPlayer player, Payloads.SubmitCodeC2S payload) {
        SecureLockConfig config = SecureLockConfig.get();
        if (!PasscodeHasher.isValidCode(payload.code(), 1, config.codeMaxLength)) {
            send(player, new Payloads.CodeResultS2C(Payloads.CodeStatus.INVALID, 0, 0));
            return;
        }
        LockManager.Locked locked = validate(player, payload.pos(), "submit_code");
        if (locked == null || !locked.lock().hasPasscode()) {
            return;
        }
        ServerLevel level = player.level();
        AccessManager.PlayerBlockKey key = AccessManager.key(player, level, locked.pos());
        long remaining = AccessManager.bruteForce().remainingLockout(key);
        if (remaining > 0) {
            send(player, new Payloads.CodeResultS2C(Payloads.CodeStatus.LOCKED_OUT, (int) Math.ceil(remaining / 1000.0), 0));
            return;
        }
        String location = AccessManager.describe(level, locked.pos());
        if (locked.lock().checkPasscode(payload.code())) {
            AccessManager.bruteForce().recordSuccess(key);
            AccessManager.sessions().grant(key, config.sessionSeconds * 1000L);
            AuditLogger.log(AuditLogger.Event.CODE_OK, player.getName().getString(), location, "");
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, locked.pos().getX() + 0.5, locked.pos().getY() + 0.8, locked.pos().getZ() + 0.5, 5, 0.3, 0.2, 0.3, 0.0);
            send(player, new Payloads.CodeResultS2C(Payloads.CodeStatus.OK, 0, 0));
            performAccess(player, level, payload.pos(), locked);
            return;
        }
        boolean lockedOut = AccessManager.bruteForce().recordFailure(key);
        if (lockedOut) {
            AuditLogger.log(AuditLogger.Event.CODE_LOCKOUT, player.getName().getString(), location, "owner=" + locked.lock().ownerName());
            send(player, new Payloads.CodeResultS2C(Payloads.CodeStatus.LOCKED_OUT, config.lockoutSeconds, 0));
        } else {
            AuditLogger.log(AuditLogger.Event.CODE_FAIL, player.getName().getString(), location, "");
            int left = config.maxFailedAttempts - AccessManager.bruteForce().failures(key);
            send(player, new Payloads.CodeResultS2C(Payloads.CodeStatus.WRONG, 0, left));
        }
    }

    /** Tras un código correcto: el teclado emite señal; un contenedor o puerta se abre. */
    private static void performAccess(ServerPlayer player, ServerLevel level, BlockPos pos, LockManager.Locked locked) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof SecureEmitterBlock emitter) {
            emitter.activate(state, level, pos, player);
            return;
        }
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        state.useWithoutItem(level, player, hit);
    }

    // ---------- Configuración ----------

    private static LockManager.@Nullable Locked validateOwner(ServerPlayer player, BlockPos pos, String packet) {
        LockManager.Locked locked = validate(player, pos, packet);
        if (locked == null) {
            return null;
        }
        if (!AccessManager.canConfigure(player, locked.lock())) {
            AuditLogger.log(AuditLogger.Event.PACKET_REJECTED, player.getName().getString(), AccessManager.describe(player.level(), pos), packet + " sin permiso");
            return null;
        }
        return locked;
    }

    private static void onSetCode(ServerPlayer player, Payloads.SetCodeC2S payload) {
        LockManager.Locked locked = validateOwner(player, payload.pos(), "set_code");
        if (locked == null) {
            return;
        }
        SecureLockConfig config = SecureLockConfig.get();
        String code = payload.code();
        if (!code.isEmpty() && !PasscodeHasher.isValidCode(code, config.codeMinLength, config.codeMaxLength)) {
            player.sendOverlayMessage(Component.translatable("message.securelock.invalid_code", config.codeMinLength, config.codeMaxLength).withStyle(ChatFormatting.RED));
            return;
        }
        ServerLevel level = player.level();
        LockManager.put(level, locked.pos(), locked.lock().withPasscode(code));
        AuditLogger.log(AuditLogger.Event.CODE_CHANGED, player.getName().getString(), AccessManager.describe(level, locked.pos()), code.isEmpty() ? "removed" : "set");
        player.sendOverlayMessage(Component.translatable(code.isEmpty() ? "message.securelock.code_removed" : "message.securelock.code_set").withStyle(ChatFormatting.GREEN));
        openConfig(player, level, locked.pos());
    }

    private static void onUpdateAllowList(ServerPlayer player, Payloads.UpdateAllowListC2S payload) {
        if (payload.action() == null || !PLAYER_NAME.matcher(payload.name()).matches()) {
            return;
        }
        LockManager.Locked locked = validateOwner(player, payload.pos(), "allow_list");
        if (locked == null) {
            return;
        }
        ServerLevel level = player.level();
        LockData lock = locked.lock();
        LockData updated;
        AuditLogger.Event event;
        switch (payload.action()) {
            case ALLOW_ADD, DENY_ADD -> {
                Optional<NameAndId> target = resolvePlayer(level.getServer(), payload.name());
                if (target.isEmpty()) {
                    player.sendOverlayMessage(Component.translatable("message.securelock.unknown_player", payload.name()).withStyle(ChatFormatting.RED));
                    return;
                }
                if (lock.isOwner(target.get().id())) {
                    return;
                }
                boolean allow = payload.action() == Payloads.ListAction.ALLOW_ADD;
                updated = allow ? lock.withAllowed(target.get().id(), target.get().name(), true) : lock.withDenied(target.get().id(), target.get().name(), true);
                event = allow ? AuditLogger.Event.ALLOW_ADD : AuditLogger.Event.DENY_ADD;
            }
            case ALLOW_REMOVE, DENY_REMOVE -> {
                boolean allow = payload.action() == Payloads.ListAction.ALLOW_REMOVE;
                Map<UUID, String> members = allow ? lock.allowed() : lock.denied();
                UUID target = members.entrySet().stream().filter(e -> e.getValue().equalsIgnoreCase(payload.name()))
                        .map(Map.Entry::getKey).findFirst().orElse(null);
                if (target == null) {
                    return;
                }
                updated = allow ? lock.withAllowed(target, payload.name(), false) : lock.withDenied(target, payload.name(), false);
                event = allow ? AuditLogger.Event.ALLOW_REMOVE : AuditLogger.Event.DENY_REMOVE;
            }
            default -> {
                return;
            }
        }
        LockManager.put(level, locked.pos(), updated);
        AuditLogger.log(event, player.getName().getString(), AccessManager.describe(level, locked.pos()), payload.name());
        openConfig(player, level, locked.pos());
    }

    private static void onSetMode(ServerPlayer player, Payloads.SetModeC2S payload) {
        LockManager.Locked locked = validateOwner(player, payload.pos(), "set_mode");
        if (locked == null) {
            return;
        }
        ServerLevel level = player.level();
        AccessMode mode = AccessMode.byId(payload.mode().toLowerCase(Locale.ROOT));
        int cardLevel = Math.max(0, Math.min(5, payload.cardLevel()));
        LockData updated = locked.lock().withMode(mode).withCardLevel(cardLevel);
        LockManager.put(level, locked.pos(), updated);
        if (mode != locked.lock().mode()) {
            AuditLogger.log(AuditLogger.Event.MODE_CHANGED, player.getName().getString(), AccessManager.describe(level, locked.pos()), mode.id());
        }
        if (cardLevel != locked.lock().cardLevel()) {
            AuditLogger.log(AuditLogger.Event.CARD_LEVEL_CHANGED, player.getName().getString(), AccessManager.describe(level, locked.pos()), String.valueOf(cardLevel));
        }
        openConfig(player, level, locked.pos());
    }

    /** Busca un jugador por nombre: conectados primero, luego jugadores ya vistos en este mundo (sin peticiones a Mojang). */
    public static Optional<NameAndId> resolvePlayer(MinecraftServer server, String name) {
        ServerPlayer online = server.getPlayerList().getPlayerByName(name);
        if (online != null) {
            return Optional.of(new NameAndId(online.getUUID(), online.getName().getString()));
        }
        ProtectedBlocksState state = ProtectedBlocksState.get(server);
        return state.findByName(name);
    }

    // ---------- Servidor -> cliente ----------

    public static void openKeypad(ServerPlayer player, ServerLevel level, BlockPos pos) {
        SecureLockConfig config = SecureLockConfig.get();
        send(player, new Payloads.OpenKeypadS2C(pos, 1, config.codeMaxLength));
    }

    public static void openConfig(ServerPlayer player, ServerLevel level, BlockPos pos) {
        LockManager.Locked locked = LockManager.find(level, pos);
        if (locked == null) {
            return;
        }
        LockData lock = locked.lock();
        BlockState state = level.getBlockState(locked.pos());
        boolean cardReader = state.getBlock() instanceof CardReaderBlock;
        // El lector de tarjetas no usa código; el resto (teclado, contenedores, puertas con candado) sí
        boolean usesCode = !cardReader && (state.getBlock() instanceof KeypadBlock || lock.kind() == LockKind.PADLOCK
                || !(state.getBlock() instanceof SecureEmitterBlock));
        SecureLockConfig config = SecureLockConfig.get();
        send(player, new Payloads.OpenConfigS2C(locked.pos(), state.getBlock().getDescriptionId(), lock.ownerName(), lock.mode().id(),
                new ArrayList<>(lock.allowed().values()), new ArrayList<>(lock.denied().values()), lock.hasPasscode(),
                lock.cardLevel(), cardReader, usesCode, config.codeMinLength, config.codeMaxLength));
    }

    public static void send(ServerPlayer player, net.minecraft.network.protocol.common.custom.CustomPacketPayload payload) {
        if (ServerPlayNetworking.canSend(player, payload.type())) {
            ServerPlayNetworking.send(player, payload);
        }
    }
}
