package com.alonex15.securelock.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** Marca los bloques propios de SecureLock. */
public interface SecureLockBlock {
    /**
     * {@code true} si el bloque gestiona por sí mismo quién puede usarlo (teclado, lector de tarjetas, escáner).
     * El manejador global de clic derecho no lo bloquea.
     */
    default boolean isSelfGuarded() {
        return false;
    }

    /** Si el bloque abre un panel de configuración nada más colocarlo (para poner el código). */
    default boolean opensConfigOnPlace() {
        return false;
    }

    /**
     * Equivalente vanilla para {@code /securelock admin unlockall} y la purga: se conserva el contenido
     * y las propiedades comunes. {@code null} = el bloque se elimina.
     */
    @Nullable BlockState toVanilla(ServerLevel level, BlockPos pos, BlockState state);
}
