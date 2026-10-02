package com.takumistudios.securemod.block;

import com.takumistudios.securemod.lock.LockManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

/**
 * Las puertas, trampillas y puertas de valla reforzadas ignoran la redstone externa:
 * solo responden a teclados, lectores y escáneres de Secure Mod del mismo propietario,
 * colocados al lado o sobre un bloque sólido adyacente.
 */
public final class RedstoneGate {
    private RedstoneGate() {
    }

    /** Emisores propios: teclado, lector de tarjetas, escáner biométrico. */
    public interface SecureEmitter {
    }

    public static boolean hasAuthorizedSignal(Level level, BlockPos pos, BlockPos lockPos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }
        LockManager.Locked locked = LockManager.getAt(serverLevel, lockPos);
        if (locked == null) {
            return false;
        }
        UUID owner = locked.lock().owner();
        for (Direction direction : Direction.values()) {
            BlockPos neighbour = pos.relative(direction);
            BlockState neighbourState = level.getBlockState(neighbour);
            if (isPoweredEmitter(neighbourState) && sameOwner(serverLevel, neighbour, owner)) {
                return true;
            }
            if (neighbourState.isRedstoneConductor(level, neighbour)) {
                for (Direction second : Direction.values()) {
                    BlockPos emitterPos = neighbour.relative(second);
                    if (emitterPos.equals(pos)) {
                        continue;
                    }
                    BlockState emitterState = level.getBlockState(emitterPos);
                    if (isPoweredEmitter(emitterState)
                            && emitterPos.relative(SecureEmitterBlock.attachedTo(emitterState)).equals(neighbour)
                            && sameOwner(serverLevel, emitterPos, owner)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean isPoweredEmitter(BlockState state) {
        return state.getBlock() instanceof SecureEmitter && state.hasProperty(ButtonBlock.POWERED) && state.getValue(ButtonBlock.POWERED);
    }

    private static boolean sameOwner(ServerLevel level, BlockPos pos, UUID owner) {
        LockManager.Locked locked = LockManager.getAt(level, pos);
        return locked != null && locked.lock().isOwner(owner);
    }
}
