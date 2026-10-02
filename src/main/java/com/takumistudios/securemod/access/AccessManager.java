package com.takumistudios.securemod.access;

import com.takumistudios.securemod.api.SecureModEvents;
import com.takumistudios.securemod.compat.ClaimsCompat;
import com.takumistudios.securemod.config.RaidWindowSchedule;
import com.takumistudios.securemod.config.SecureModConfig;
import com.takumistudios.securemod.data.ProtectedBlocksState;
import com.takumistudios.securemod.lock.LockData;
import com.takumistudios.securemod.lock.LockKind;
import com.takumistudios.securemod.lock.LockManager;
import com.takumistudios.securemod.security.AccessMode;
import com.takumistudios.securemod.security.AuditLogger;
import com.takumistudios.securemod.security.BruteForceTracker;
import com.takumistudios.securemod.security.RateLimiter;
import com.takumistudios.securemod.security.SessionTracker;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;

import java.time.LocalDateTime;
import java.util.UUID;

/** Reglas de autorización. Todo se decide en el servidor. */
public final class AccessManager {
    /** Clave jugador + bloque para sesiones y fuerza bruta. */
    public record PlayerBlockKey(UUID player, GlobalPos pos) {
    }

    private static final SessionTracker<PlayerBlockKey> SESSIONS = new SessionTracker<>();
    private static final BruteForceTracker<PlayerBlockKey> BRUTE_FORCE = new BruteForceTracker<>(3, 30_000);
    private static final RateLimiter<UUID> RATE_LIMITER = new RateLimiter<>(10);

    private static long raidCheckAt;
    private static boolean raidActive;

    private AccessManager() {
    }

    public static void reconfigure() {
        SecureModConfig config = SecureModConfig.get();
        BRUTE_FORCE.configure(config.maxFailedAttempts, config.lockoutSeconds * 1000L);
        RATE_LIMITER.setMaxPerSecond(config.maxPacketsPerSecond);
        raidCheckAt = 0;
    }

    public static SessionTracker<PlayerBlockKey> sessions() {
        return SESSIONS;
    }

    public static BruteForceTracker<PlayerBlockKey> bruteForce() {
        return BRUTE_FORCE;
    }

    public static RateLimiter<UUID> rateLimiter() {
        return RATE_LIMITER;
    }

    public static PlayerBlockKey key(Player player, Level level, BlockPos pos) {
        return new PlayerBlockKey(player.getUUID(), GlobalPos.of(level.dimension(), pos.immutable()));
    }

    /** ¿Puede usar (abrir, activar) el bloque? */
    public static boolean canUse(Player player, Level level, LockManager.Locked locked) {
        LockData lock = locked.lock();
        if (PermissionHelper.canBypass(player)) {
            return true;
        }
        if (lock.isCorrupt()) {
            return false;
        }
        UUID uuid = effectiveUuid(player, lock);
        if (uuid == null) {
            return false;
        }
        if (lock.isOwner(uuid)) {
            return true;
        }
        if (lock.denied().containsKey(uuid)) {
            return false;
        }
        if (lock.mode() == AccessMode.PUBLIC || lock.allowed().containsKey(uuid)) {
            return true;
        }
        if (level.getServer() != null && ProtectedBlocksState.get(level.getServer()).isTrusted(lock.owner(), uuid)) {
            return true;
        }
        if (lock.mode() == AccessMode.SHARED && SecureModConfig.get().allowTeamAccess && sameTeam(player, lock)) {
            return true;
        }
        if (player instanceof ServerPlayer serverPlayer && level instanceof ServerLevel serverLevel
                && ClaimsCompat.hasClaimAccess(serverPlayer, serverLevel, locked.pos())) {
            return true;
        }
        return SESSIONS.isActive(key(player, level, locked.pos()));
    }

    /** Lista blanca de un bloque o escáner (sin código ni sesión): propietario, permitidos, confianza y equipo. */
    public static boolean isWhitelisted(Player player, Level level, LockData lock) {
        if (PermissionHelper.canBypass(player)) {
            return true;
        }
        if (lock.isCorrupt()) {
            return false;
        }
        UUID uuid = player.getUUID();
        if (lock.isOwner(uuid)) {
            return true;
        }
        if (lock.denied().containsKey(uuid)) {
            return false;
        }
        if (lock.allowed().containsKey(uuid)) {
            return true;
        }
        if (level.getServer() != null && ProtectedBlocksState.get(level.getServer()).isTrusted(lock.owner(), uuid)) {
            return true;
        }
        return lock.mode() == AccessMode.SHARED && SecureModConfig.get().allowTeamAccess && sameTeam(player, lock);
    }

    /** ¿Puede romper el bloque protegido? */
    public static boolean canBreak(Player player, LockManager.Locked locked) {
        if (PermissionHelper.canBypass(player) || isRaidWindowActive()) {
            return true;
        }
        LockData lock = locked.lock();
        if (lock.isCorrupt()) {
            return false;
        }
        // Los bloques propios solo los rompe el propietario en creativo o con el Universal Block Remover
        if (lock.kind() == LockKind.BLOCK) {
            return lock.isOwner(player.getUUID()) && player.isCreative();
        }
        return lock.isOwner(player.getUUID());
    }

    /** ¿Puede cambiar la configuración (código, lista, modo)? */
    public static boolean canConfigure(Player player, LockData lock) {
        return (!lock.isCorrupt() && lock.isOwner(player.getUUID())) || PermissionHelper.canBypass(player);
    }

    /** Si un admin accede a un bloque ajeno gracias al bypass, se registra. */
    public static void auditBypass(Player player, Level level, LockManager.Locked locked) {
        if (!locked.lock().isOwner(player.getUUID()) && PermissionHelper.canBypass(player)) {
            AuditLogger.log(AuditLogger.Event.ADMIN_BYPASS, player.getName().getString(), describe(level, locked.pos()), "owner=" + locked.lock().ownerName());
        }
    }

    /** UUID con el que se evalúa al jugador. Los FakePlayer de otros mods se tratan según la config. */
    private static UUID effectiveUuid(Player player, LockData lock) {
        if (player instanceof FakePlayer fakePlayer) {
            SecureModConfig config = SecureModConfig.get();
            if (config.allowedFakePlayerNames.contains(fakePlayer.getGameProfile().name())) {
                return lock.owner();
            }
            return config.fakePlayersUseOwnerPermissions ? fakePlayer.getUUID() : null;
        }
        return player.getUUID();
    }

    private static boolean sameTeam(Player player, LockData lock) {
        PlayerTeam team = player.getTeam();
        if (team == null) {
            return false;
        }
        Team ownerTeam = player.level().getScoreboard().getPlayersTeam(lock.ownerName());
        return ownerTeam != null && ownerTeam.getName().equals(team.getName());
    }

    public static boolean isRaidWindowActive() {
        SecureModConfig config = SecureModConfig.get();
        if (!config.raidWindowsEnabled || config.raidWindows.isEmpty()) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (now >= raidCheckAt) {
            raidActive = RaidWindowSchedule.isActive(config.raidWindows, LocalDateTime.now());
            raidCheckAt = now + 1000;
        }
        return raidActive;
    }

    /** Aviso en la barra de acción (no spam en el chat) + sonido de denegación. */
    public static void deny(ServerPlayer player, LockManager.Locked locked, String reasonKey) {
        player.sendOverlayMessage(Component.translatable(reasonKey, locked.lock().ownerName()).withStyle(ChatFormatting.RED));
        ServerLevel level = player.level();
        level.playSound(null, locked.pos(), SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 0.6F, 0.6F);
        level.sendParticles(ParticleTypes.SMOKE, locked.pos().getX() + 0.5, locked.pos().getY() + 0.6, locked.pos().getZ() + 0.5, 6, 0.25, 0.25, 0.25, 0.01);
        SecureModEvents.ACCESS_DENIED.invoker().onAccessDenied(player, level, locked.pos(), locked.lock().owner());
    }

    public static String describe(Level level, BlockPos pos) {
        return level.dimension().identifier() + " " + pos.getX() + " " + pos.getY() + " " + pos.getZ();
    }

    /** Distancia máxima de interacción para validar paquetes. */
    public static boolean isWithinReach(ServerPlayer player, BlockPos pos) {
        double max = SecureModConfig.get().maxInteractDistance;
        return player.position().distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= max * max;
    }

    public static void cleanup() {
        SESSIONS.cleanup();
        BRUTE_FORCE.cleanup();
    }

    public static void forgetPlayer(UUID uuid) {
        RATE_LIMITER.forget(uuid);
    }
}
