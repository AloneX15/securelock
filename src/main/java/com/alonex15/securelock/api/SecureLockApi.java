package com.alonex15.securelock.api;

import com.alonex15.securelock.access.AccessManager;
import com.alonex15.securelock.lock.LockManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.UUID;

/**
 * API pública y estable para que otros mods respeten las protecciones de SecureLock.
 * Úsala con {@code FabricLoader.getInstance().isModLoaded("securelock")} para que tu mod no dependa de SecureLock.
 *
 * <p>Solo devuelve información en el servidor. Nunca carga chunks.
 */
public final class SecureLockApi {
    private SecureLockApi() {
    }

    /** ¿Está protegido el bloque? */
    public static boolean isProtected(Level level, BlockPos pos) {
        return LockManager.isProtected(level, pos);
    }

    /** ¿Puede este jugador usar el bloque? Devuelve true si el bloque no está protegido. */
    public static boolean canAccess(Player player, BlockPos pos) {
        LockManager.Locked locked = LockManager.find(player.level(), pos);
        return locked == null || AccessManager.canUse(player, player.level(), locked);
    }

    /** ¿Puede este jugador romper el bloque? Devuelve true si el bloque no está protegido. */
    public static boolean canBreak(Player player, BlockPos pos) {
        LockManager.Locked locked = LockManager.find(player.level(), pos);
        return locked == null || AccessManager.canBreak(player, locked);
    }

    /** Propietario del bloque protegido, si lo hay. */
    public static Optional<UUID> getOwner(Level level, BlockPos pos) {
        LockManager.Locked locked = LockManager.find(level, pos);
        return locked == null ? Optional.empty() : Optional.of(locked.lock().owner());
    }
}
