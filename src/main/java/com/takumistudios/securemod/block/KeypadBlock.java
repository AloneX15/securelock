package com.takumistudios.securemod.block;

import com.takumistudios.securemod.access.AccessManager;
import com.takumistudios.securemod.config.SecureModConfig;
import com.takumistudios.securemod.lock.LockManager;
import com.takumistudios.securemod.network.ServerNetworking;
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

/** Teclado numérico: al introducir el código correcto emite redstone durante N ticks. */
public class KeypadBlock extends SecureEmitterBlock {
    public KeypadBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected int signalTicks() {
        return SecureModConfig.get().keypadSignalTicks;
    }

    @Override
    public boolean opensConfigOnPlace() {
        return true;
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
        if (!locked.lock().hasPasscode()) {
            if (AccessManager.canConfigure(player, locked.lock())) {
                ServerNetworking.openConfig(serverPlayer, serverLevel, pos);
            } else {
                serverPlayer.sendOverlayMessage(Component.translatable("message.securemod.keypad_not_set").withStyle(ChatFormatting.YELLOW));
            }
            return InteractionResult.SUCCESS;
        }
        if (AccessManager.isWhitelisted(player, level, locked.lock())) {
            activate(state, level, pos, player);
        } else {
            ServerNetworking.openKeypad(serverPlayer, serverLevel, pos);
        }
        return InteractionResult.SUCCESS;
    }
}
