package com.alonex15.securelock.block;

import com.alonex15.securelock.SecureLock;
import com.alonex15.securelock.access.AccessManager;
import com.alonex15.securelock.access.PermissionHelper;
import com.alonex15.securelock.api.SecureLockEvents;
import com.alonex15.securelock.config.SecureLockConfig;
import com.alonex15.securelock.data.ProtectedBlocksState;
import com.alonex15.securelock.lock.LockData;
import com.alonex15.securelock.lock.LockKind;
import com.alonex15.securelock.lock.LockManager;
import com.alonex15.securelock.network.ServerNetworking;
import com.alonex15.securelock.security.AuditLogger;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Lógica compartida por los bloques propios: reclamar al colocar, limpiar al quitar, convertir a vanilla. */
public final class OwnedBlocks {
    private OwnedBlocks() {
    }

    /** Se llama desde {@code setPlacedBy}: el jugador que coloca el bloque pasa a ser su propietario. */
    public static void claim(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer) {
        if (!(level instanceof ServerLevel serverLevel) || !(placer instanceof ServerPlayer player)) {
            return;
        }
        try {
            BlockPos canonical = LockManager.canonical(pos, state);
            LockData lock = LockData.create(player.getUUID(), player.getName().getString(), LockKind.BLOCK, LockManager.blockId(state));
            LockManager.put(serverLevel, canonical, lock);
            AuditLogger.log(AuditLogger.Event.PLACE, player.getName().getString(), AccessManager.describe(level, canonical), LockManager.blockId(state));
            SecureLockEvents.BLOCK_LOCKED.invoker().onBlockLocked(player, serverLevel, canonical);
            if (state.getBlock() instanceof SecureLockBlock secure && secure.opensConfigOnPlace()) {
                ServerNetworking.openConfig(player, serverLevel, canonical);
            }
        } catch (RuntimeException e) {
            SecureLock.warnOnce("claim", "Error al reclamar un bloque protegido", e);
        }
    }

    /** ¿Ha alcanzado el jugador el límite de bloques protegidos? Avisa en la barra de acción. */
    public static boolean isAtLimit(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer) || player.isCreative()) {
            return false;
        }
        // El nodo entero securelock:limit (LuckPerms meta, por rango) tiene prioridad sobre la config
        Integer rankLimit = PermissionHelper.intValue(player, PermissionHelper.LIMIT);
        int max = rankLimit != null ? rankLimit : SecureLockConfig.get().maxProtectedBlocksPerPlayer;
        if (max <= 0) {
            return false;
        }
        int count = ProtectedBlocksState.get(serverPlayer.level().getServer()).countOf(player.getUUID());
        if (count >= max) {
            serverPlayer.sendOverlayMessage(Component.translatable("message.securelock.limit_reached", max).withStyle(ChatFormatting.RED));
            return true;
        }
        return false;
    }

    /** Se llama cuando un bloque propio desaparece del mundo. */
    public static void onRemoved(ServerLevel level, BlockPos pos, BlockState state) {
        try {
            LockManager.remove(level, LockManager.canonical(pos, state));
        } catch (RuntimeException e) {
            SecureLock.warnOnce("remove", "Error al limpiar un bloque protegido", e);
        }
    }

    /**
     * Convierte un bloque propio en su equivalente vanilla conservando el contenido (unlockall, purga).
     * Devuelve true si se convirtió o eliminó.
     */
    public static boolean convertToVanilla(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof SecureLockBlock secure)) {
            return false;
        }
        BlockPos lower = LockManager.canonical(pos, state);
        LockManager.remove(level, lower);
        BlockState lowerState = level.getBlockState(lower);
        BlockState replacement = secure.toVanilla(level, lower, lowerState);
        if (replacement == null) {
            level.removeBlock(lower, false);
            return true;
        }
        // Se guarda el contenido y se vacía el contenedor antes del cambio para no duplicar ítems
        List<ItemStack> contents = new ArrayList<>();
        BlockEntity blockEntity = level.getBlockEntity(lower);
        if (blockEntity instanceof Container container) {
            for (int i = 0; i < container.getContainerSize(); i++) {
                contents.add(container.getItem(i).copy());
            }
            container.clearContent();
        }
        int flags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        if (lowerState.getBlock() instanceof DoorBlock) {
            BlockPos upper = lower.above();
            level.setBlock(lower, replacement, flags);
            level.setBlock(upper, replacement.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), flags);
        } else {
            level.setBlock(lower, replacement, flags);
        }
        if (!contents.isEmpty() && level.getBlockEntity(lower) instanceof Container newContainer) {
            for (int i = 0; i < Math.min(contents.size(), newContainer.getContainerSize()); i++) {
                newContainer.setItem(i, contents.get(i));
            }
            newContainer.setChanged();
        }
        return true;
    }
}
