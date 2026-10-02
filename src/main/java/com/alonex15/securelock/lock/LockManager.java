package com.alonex15.securelock.lock;

import com.alonex15.securelock.SecureLock;
import com.alonex15.securelock.block.SecureLockBlock;
import com.alonex15.securelock.config.SecureLockConfig;
import com.alonex15.securelock.data.ProtectedBlocksState;
import com.alonex15.securelock.registry.ModAttachments;
import com.alonex15.securelock.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.Nullable;

/**
 * Acceso a las protecciones guardadas en los chunks. Solo funciona en el servidor y
 * <b>nunca carga chunks</b>: si el chunk no está cargado, el resultado es "sin protección encontrada".
 */
public final class LockManager {
    private LockManager() {
    }

    /** Un bloque protegido: posición canónica donde está guardado el candado y sus datos. */
    public record Locked(BlockPos pos, LockData lock) {
    }

    /** Posición donde se guarda el candado: la mitad inferior en las puertas. */
    public static BlockPos canonical(BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof DoorBlock && state.hasProperty(DoorBlock.HALF) && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER) {
            return pos.below();
        }
        return pos;
    }

    /** Busca la protección que afecta a {@code pos}, incluida la otra mitad de un cofre doble. */
    public static @Nullable Locked find(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return null;
        }
        LevelChunk chunk = loadedChunk(serverLevel, pos);
        if (chunk == null) {
            return null;
        }
        BlockState state = chunk.getBlockState(pos);
        BlockPos canonical = canonical(pos, state);
        Locked locked = getAt(serverLevel, canonical);
        if (locked != null) {
            return locked;
        }
        if (state.getBlock() instanceof ChestBlock && state.hasProperty(ChestBlock.TYPE) && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            BlockPos other = pos.relative(ChestBlock.getConnectedDirection(state));
            BlockState otherState = serverLevel.getBlockState(other);
            if (otherState.getBlock() instanceof ChestBlock) {
                return getAt(serverLevel, other);
            }
        }
        return null;
    }

    public static boolean isProtected(Level level, BlockPos pos) {
        return find(level, pos) != null;
    }

    /** Lee exactamente la posición indicada, validando que el bloque siga existiendo. */
    public static @Nullable Locked getAt(ServerLevel level, BlockPos pos) {
        LevelChunk chunk = loadedChunk(level, pos);
        if (chunk == null) {
            return null;
        }
        ChunkLocks locks = chunk.getAttached(ModAttachments.CHUNK_LOCKS);
        if (locks == null) {
            return null;
        }
        LockData lock = locks.get(pos);
        if (lock == null) {
            return null;
        }
        BlockState state = chunk.getBlockState(pos);
        if (!isStillValid(lock, state)) {
            // El bloque ya no existe (o se cambió por otro medio): se limpia el candado huérfano
            remove(level, pos);
            return null;
        }
        return new Locked(pos.immutable(), lock);
    }

    private static boolean isStillValid(LockData lock, BlockState state) {
        if (state.isAir()) {
            return false;
        }
        if (lock.kind() == LockKind.BLOCK) {
            return state.getBlock() instanceof SecureLockBlock;
        }
        // Un candado sigue siendo válido mientras el bloque sea bloqueable (p. ej. una puerta de cobre que se oxida)
        return state.getBlock() instanceof SecureLockBlock || isLockable(state);
    }

    public static void put(ServerLevel level, BlockPos pos, LockData lock) {
        LevelChunk chunk = level.getChunkAt(pos);
        ChunkLocks locks = chunk.getAttachedOrElse(ModAttachments.CHUNK_LOCKS, ChunkLocks.EMPTY);
        LockData previous = locks.get(pos);
        chunk.setAttached(ModAttachments.CHUNK_LOCKS, locks.with(pos, lock));
        ProtectedBlocksState index = ProtectedBlocksState.get(level.getServer());
        GlobalPos globalPos = GlobalPos.of(level.dimension(), pos.immutable());
        if (previous != null && !previous.owner().equals(lock.owner())) {
            index.remove(previous.owner(), globalPos);
        }
        index.add(lock.owner(), lock.ownerName(), globalPos);
    }

    public static @Nullable LockData remove(ServerLevel level, BlockPos pos) {
        LevelChunk chunk = loadedChunk(level, pos);
        if (chunk == null) {
            return null;
        }
        ChunkLocks locks = chunk.getAttached(ModAttachments.CHUNK_LOCKS);
        if (locks == null) {
            return null;
        }
        LockData previous = locks.get(pos);
        if (previous == null) {
            return null;
        }
        ChunkLocks updated = locks.without(pos);
        if (updated.isEmpty()) {
            chunk.removeAttached(ModAttachments.CHUNK_LOCKS);
        } else {
            chunk.setAttached(ModAttachments.CHUNK_LOCKS, updated);
        }
        // removeAttached no siempre marca el chunk como modificado
        chunk.markUnsaved();
        ProtectedBlocksState.get(level.getServer()).remove(previous.owner(), GlobalPos.of(level.dimension(), pos.immutable()));
        return previous;
    }

    /** ¿Puede el Padlock bloquear este bloque? Tags de datapack + listas de la config. */
    public static boolean isLockable(BlockState state) {
        SecureLockConfig config = SecureLockConfig.get();
        String id = blockId(state);
        if (state.is(ModTags.NEVER_LOCK) || config.neverLockableBlocks.contains(id)) {
            return false;
        }
        return state.is(ModTags.LOCKABLE) || config.extraLockableBlocks.contains(id);
    }

    public static String blockId(BlockState state) {
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
    }

    /** Devuelve el chunk solo si ya está cargado. */
    public static @Nullable LevelChunk loadedChunk(ServerLevel level, BlockPos pos) {
        try {
            return level.getChunkSource().getChunkNow(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
        } catch (RuntimeException e) {
            SecureLock.warnOnce("chunk_lookup", "Error al leer un chunk para comprobar una protección", e);
            return null;
        }
    }
}
