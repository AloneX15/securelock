package com.takumistudios.securemod.api;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/** Eventos públicos para que otros mods reaccionen a las protecciones. */
public final class SecureModEvents {
    private SecureModEvents() {
    }

    /** Se lanza cuando a un jugador se le deniega el acceso a un bloque protegido. */
    public static final Event<AccessDenied> ACCESS_DENIED = EventFactory.createArrayBacked(AccessDenied.class,
            listeners -> (player, level, pos, owner) -> {
                for (AccessDenied listener : listeners) {
                    listener.onAccessDenied(player, level, pos, owner);
                }
            });

    /** Se lanza cuando un bloque queda protegido (bloque propio colocado o candado puesto). */
    public static final Event<BlockLocked> BLOCK_LOCKED = EventFactory.createArrayBacked(BlockLocked.class,
            listeners -> (player, level, pos) -> {
                for (BlockLocked listener : listeners) {
                    listener.onBlockLocked(player, level, pos);
                }
            });

    /** Se lanza cuando se quita una protección. {@code player} puede ser null (comando o purga). */
    public static final Event<BlockUnlocked> BLOCK_UNLOCKED = EventFactory.createArrayBacked(BlockUnlocked.class,
            listeners -> (player, level, pos) -> {
                for (BlockUnlocked listener : listeners) {
                    listener.onBlockUnlocked(player, level, pos);
                }
            });

    @FunctionalInterface
    public interface AccessDenied {
        void onAccessDenied(ServerPlayer player, ServerLevel level, BlockPos pos, UUID owner);
    }

    @FunctionalInterface
    public interface BlockLocked {
        void onBlockLocked(ServerPlayer player, ServerLevel level, BlockPos pos);
    }

    @FunctionalInterface
    public interface BlockUnlocked {
        void onBlockUnlocked(ServerPlayer player, ServerLevel level, BlockPos pos);
    }
}
