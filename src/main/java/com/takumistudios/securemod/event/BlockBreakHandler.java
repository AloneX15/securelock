package com.takumistudios.securemod.event;

import com.takumistudios.securemod.SecureMod;
import com.takumistudios.securemod.access.AccessManager;
import com.takumistudios.securemod.api.SecureModEvents;
import com.takumistudios.securemod.config.SecureModConfig;
import com.takumistudios.securemod.lock.LockData;
import com.takumistudios.securemod.lock.LockManager;
import com.takumistudios.securemod.security.AuditLogger;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import org.jspecify.annotations.Nullable;

/** Romper bloques ({@code PlayerBlockBreakEvents}). */
public final class BlockBreakHandler {
    private BlockBreakHandler() {
    }

    /** BEFORE: cancela si el jugador no es el propietario/admin. También protege el soporte de una puerta protegida. */
    public static boolean beforeBreak(Level level, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity) {
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return true;
        }
        try {
            LockManager.Locked locked = LockManager.find(serverLevel, pos);
            if (locked != null) {
                if (AccessManager.canBreak(player, locked)) {
                    AccessManager.auditBypass(player, level, locked);
                    return true;
                }
                deny(serverPlayer, serverLevel, locked, state);
                return false;
            }
            LockManager.Locked supported = supportedDoor(serverLevel, pos);
            if (supported != null && !AccessManager.canBreak(player, supported)) {
                deny(serverPlayer, serverLevel, supported, state);
                return false;
            }
            return true;
        } catch (RuntimeException e) {
            SecureMod.warnOnce("break_before", "Error al comprobar la rotura de un bloque", e);
            return !LockManager.isProtected(level, pos);
        }
    }

    /** AFTER: si se rompió un bloque protegido (por su propietario o un admin), se elimina la protección. */
    public static void afterBreak(Level level, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        try {
            BlockPos canonical = LockManager.canonical(pos, state);
            LockData removed = LockManager.remove(serverLevel, canonical);
            if (removed != null && moveToOtherChestHalf(serverLevel, pos, state, removed)) {
                return;
            }
            if (removed != null) {
                AuditLogger.log(AuditLogger.Event.BREAK, player.getName().getString(), AccessManager.describe(level, canonical),
                        LockManager.blockId(state) + " owner=" + removed.ownerName());
                if (player instanceof ServerPlayer serverPlayer) {
                    SecureModEvents.BLOCK_UNLOCKED.invoker().onBlockUnlocked(serverPlayer, serverLevel, canonical);
                }
            }
        } catch (RuntimeException e) {
            SecureMod.warnOnce("break_after", "Error al limpiar un bloque protegido roto", e);
        }
    }

    /** En un cofre doble con candado, si se rompe la mitad que guardaba el candado, la otra mitad sigue protegida. */
    private static boolean moveToOtherChestHalf(ServerLevel level, BlockPos pos, BlockState state, LockData lock) {
        if (!(state.getBlock() instanceof ChestBlock) || !state.hasProperty(ChestBlock.TYPE) || state.getValue(ChestBlock.TYPE) == ChestType.SINGLE) {
            return false;
        }
        BlockPos other = pos.relative(ChestBlock.getConnectedDirection(state));
        BlockState otherState = level.getBlockState(other);
        if (!(otherState.getBlock() instanceof ChestBlock) || LockManager.getAt(level, other) != null) {
            return false;
        }
        LockManager.put(level, other, lock.withBlockId(LockManager.blockId(otherState)));
        return true;
    }

    /**
     * Ctrl + clic central en creativo copia los datos del BlockEntity (el contenido de un cofre) al ítem.
     * Si el jugador no tiene acceso, recibe el ítem sin datos.
     */
    public static @Nullable ItemStack onPickBlock(ServerPlayer player, BlockPos pos, BlockState state, boolean includeData) {
        if (!includeData) {
            return null;
        }
        try {
            LockManager.Locked locked = LockManager.find(player.level(), pos);
            if (locked == null || AccessManager.canUse(player, player.level(), locked)) {
                return null;
            }
            return new ItemStack(state.getBlock().asItem());
        } catch (RuntimeException e) {
            SecureMod.warnOnce("pick_block", "Error al comprobar el pick block", e);
            return null;
        }
    }

    /** Si encima de {@code pos} hay una puerta protegida, devuelve su protección (el soporte también se protege). */
    public static LockManager.@Nullable Locked supportedDoor(ServerLevel level, BlockPos pos) {
        if (!SecureModConfig.get().protectSupportBlocks) {
            return null;
        }
        BlockPos above = pos.above();
        BlockState aboveState = level.getBlockState(above);
        if (aboveState.getBlock() instanceof DoorBlock && aboveState.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER) {
            return LockManager.getAt(level, above);
        }
        return null;
    }

    private static void deny(ServerPlayer player, ServerLevel level, LockManager.Locked locked, BlockState state) {
        AccessManager.deny(player, locked, "message.securemod.cannot_break");
        AuditLogger.log(AuditLogger.Event.ACCESS_DENIED, player.getName().getString(), AccessManager.describe(level, locked.pos()), "break " + LockManager.blockId(state));
    }
}
