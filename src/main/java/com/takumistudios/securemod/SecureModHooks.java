package com.takumistudios.securemod;

import com.takumistudios.securemod.access.AccessManager;
import com.takumistudios.securemod.config.SecureModConfig;
import com.takumistudios.securemod.event.BlockBreakHandler;
import com.takumistudios.securemod.lock.LockManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Lógica que llaman los mixins. Cada mixin contiene solo una llamada aquí, así la lógica se prueba aparte
 * y el mixin queda pequeño. Todos los métodos son a prueba de fallos: si algo lanza una excepción,
 * se registra una vez y se aplica el comportamiento seguro (el bloque protegido sigue protegido).
 */
public final class SecureModHooks {
    /** Posición de la tolva que está moviendo ítems ahora mismo (para permitir tolvas del mismo propietario). */
    private static final ThreadLocal<BlockPos> HOPPER_SOURCE = new ThreadLocal<>();

    private SecureModHooks() {
    }

    private static boolean enabled(String feature) {
        // Solo la config desactiva funciones: la verificación de mixins es informativa y nunca debilita la protección
        return !SecureModConfig.get().isFeatureDisabled(feature);
    }

    // ---------- Explosiones ----------

    /** Quita de la lista de una explosión los bloques protegidos (y el soporte de puertas protegidas). */
    public static List<BlockPos> filterExplosion(ServerLevel level, List<BlockPos> positions) {
        if (!enabled("explosion_protection") || !SecureModConfig.get().explosionProof || AccessManager.isRaidWindowActive()) {
            return positions;
        }
        try {
            List<BlockPos> result = null;
            for (int i = 0; i < positions.size(); i++) {
                BlockPos pos = positions.get(i);
                boolean protectedPos = LockManager.isProtected(level, pos) || BlockBreakHandler.supportedDoor(level, pos) != null;
                if (protectedPos && result == null) {
                    result = new ArrayList<>(positions.subList(0, i));
                } else if (!protectedPos && result != null) {
                    result.add(pos);
                }
            }
            return result == null ? positions : result;
        } catch (RuntimeException e) {
            SecureMod.warnOnce("explosion", "Error al filtrar una explosión", e);
            return positions;
        }
    }

    // ---------- Tolvas, minecart-tolva, droppers ----------

    public static void setHopperSource(@Nullable BlockPos pos) {
        if (pos == null) {
            HOPPER_SOURCE.remove();
        } else {
            HOPPER_SOURCE.set(pos);
        }
    }

    /** Una tolva no puede extraer ni insertar en un contenedor protegido (salvo tolva del mismo propietario). */
    public static @Nullable Container filterContainer(Level level, BlockPos pos, @Nullable Container container) {
        if (container == null || !(level instanceof ServerLevel serverLevel) || !enabled("hopper_protection")) {
            return container;
        }
        try {
            LockManager.Locked locked = LockManager.find(serverLevel, pos);
            if (locked == null) {
                return container;
            }
            BlockPos source = HOPPER_SOURCE.get();
            if (source != null && SecureModConfig.get().allowHoppersFromSameOwner && !locked.lock().isCorrupt()) {
                LockManager.Locked sourceLock = LockManager.find(serverLevel, source);
                if (sourceLock != null && sourceLock.lock().isOwner(locked.lock().owner())) {
                    return container;
                }
            }
            return null;
        } catch (RuntimeException e) {
            SecureMod.warnOnce("hopper", "Error al comprobar una tolva", e);
            return null;
        }
    }

    // ---------- Pistones ----------

    /** ¿Debe fallar el empuje del pistón porque mueve o destruye un bloque protegido? */
    public static boolean pistonBlocked(Level level, List<BlockPos> toPush, List<BlockPos> toDestroy) {
        if (!(level instanceof ServerLevel serverLevel) || !enabled("piston_protection")) {
            return false;
        }
        try {
            for (BlockPos pos : toPush) {
                if (LockManager.isProtected(serverLevel, pos) || BlockBreakHandler.supportedDoor(serverLevel, pos) != null) {
                    return true;
                }
            }
            for (BlockPos pos : toDestroy) {
                if (LockManager.isProtected(serverLevel, pos) || BlockBreakHandler.supportedDoor(serverLevel, pos) != null) {
                    return true;
                }
            }
            return false;
        } catch (RuntimeException e) {
            SecureMod.warnOnce("piston", "Error al comprobar un pistón", e);
            return false;
        }
    }

    // ---------- Fuego ----------

    /** ¿Puede el fuego quemar/reemplazar este bloque? */
    public static boolean canFireDestroy(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel) || !enabled("fire_protection")) {
            return true;
        }
        try {
            return !LockManager.isProtected(serverLevel, pos) && BlockBreakHandler.supportedDoor(serverLevel, pos) == null;
        } catch (RuntimeException e) {
            SecureMod.warnOnce("fire", "Error al comprobar el fuego", e);
            return true;
        }
    }

    // ---------- Mobs (wither, ender dragon) ----------

    /** ¿Puede un mob destruir este bloque? */
    public static boolean canMobDestroy(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel) || !enabled("mob_protection")) {
            return true;
        }
        try {
            return !LockManager.isProtected(serverLevel, pos) && BlockBreakHandler.supportedDoor(serverLevel, pos) == null;
        } catch (RuntimeException e) {
            SecureMod.warnOnce("mob", "Error al comprobar un mob", e);
            return true;
        }
    }

    // ---------- Comparadores ----------

    public static int filterComparator(Level level, BlockPos pos, int signal) {
        if (signal <= 0 || !SecureModConfig.get().comparatorOutputHidden || !enabled("comparator_protection")) {
            return signal;
        }
        try {
            return LockManager.isProtected(level, pos) ? 0 : signal;
        } catch (RuntimeException e) {
            SecureMod.warnOnce("comparator", "Error al comprobar un comparador", e);
            return signal;
        }
    }

    // ---------- Transfer API (mods de almacenamiento) ----------

    /** ¿Debe la Transfer API devolver "sin almacenamiento" para este bloque? */
    public static boolean blockTransfer(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel) || !enabled("transfer_protection")) {
            return false;
        }
        try {
            return LockManager.isProtected(serverLevel, pos);
        } catch (RuntimeException e) {
            SecureMod.warnOnce("transfer", "Error al comprobar la Transfer API", e);
            return true;
        }
    }

    // ---------- Gólems de cobre ----------

    /** ¿Puede un gólem de cobre (u otro mob) sacar o meter ítems en este contenedor? */
    public static boolean canMobUseContainer(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel) || !enabled("golem_protection")) {
            return true;
        }
        try {
            return !LockManager.isProtected(serverLevel, pos);
        } catch (RuntimeException e) {
            SecureMod.warnOnce("golem", "Error al comprobar un gólem", e);
            return false;
        }
    }
}
