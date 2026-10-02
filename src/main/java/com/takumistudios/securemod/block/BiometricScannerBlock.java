package com.takumistudios.securemod.block;

import com.takumistudios.securemod.access.AccessManager;
import com.takumistudios.securemod.config.SecureModConfig;
import com.takumistudios.securemod.lock.LockManager;
import com.takumistudios.securemod.security.AuditLogger;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Escáner biométrico (retina): se activa solo para el propietario y los jugadores de su lista blanca. */
public class BiometricScannerBlock extends SecureEmitterBlock {
    public BiometricScannerBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected int signalTicks() {
        return SecureModConfig.get().scannerSignalTicks;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }
        LockManager.Locked locked = LockManager.getAt(serverLevel, pos);
        if (locked == null) {
            return InteractionResult.PASS;
        }
        if (state.getValue(POWERED)) {
            return InteractionResult.CONSUME;
        }
        if (AccessManager.isWhitelisted(player, level, locked.lock())) {
            serverPlayer.sendOverlayMessage(Component.translatable("message.securemod.scan_ok", player.getName()).withStyle(ChatFormatting.GREEN));
            activate(state, level, pos, player);
        } else {
            serverPlayer.sendOverlayMessage(Component.translatable("message.securemod.scan_denied").withStyle(ChatFormatting.RED));
            playDenied(level, pos);
            AuditLogger.log(AuditLogger.Event.ACCESS_DENIED, player.getName().getString(), AccessManager.describe(level, pos), "retina");
        }
        return InteractionResult.SUCCESS;
    }
}
