package com.takumistudios.securemod.block;

import com.takumistudios.securemod.config.SecureModConfig;
import com.takumistudios.securemod.item.KeycardItem;
import com.takumistudios.securemod.lock.LockManager;
import com.takumistudios.securemod.registry.ModComponents;
import com.takumistudios.securemod.security.AuditLogger;
import com.takumistudios.securemod.access.AccessManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Lector de tarjetas: se activa con una Keycard vinculada al propietario y de nivel suficiente. */
public class CardReaderBlock extends SecureEmitterBlock {
    public CardReaderBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected int signalTicks() {
        return SecureModConfig.get().cardReaderSignalTicks;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendOverlayMessage(Component.translatable("message.securemod.insert_card").withStyle(ChatFormatting.YELLOW));
        }
        return InteractionResult.SUCCESS;
    }

    /** Pasa una tarjeta por el lector. */
    public InteractionResult swipe(BlockState state, ServerLevel level, BlockPos pos, ServerPlayer player, ItemStack stack) {
        LockManager.Locked locked = LockManager.getAt(level, pos);
        if (locked == null || !(stack.getItem() instanceof KeycardItem card)) {
            return InteractionResult.PASS;
        }
        if (state.getValue(POWERED)) {
            return InteractionResult.CONSUME;
        }
        int required = Math.max(1, locked.lock().cardLevel());
        ModComponents.CardLink link = stack.get(ModComponents.CARD_LINK);
        if (link == null) {
            player.sendOverlayMessage(Component.translatable("message.securemod.card_unlinked").withStyle(ChatFormatting.RED));
            playDenied(level, pos);
            return InteractionResult.FAIL;
        }
        if (!link.owner().equals(locked.lock().owner()) || card.level() < required || locked.lock().isCorrupt()) {
            player.sendOverlayMessage(Component.translatable("message.securemod.card_denied", required).withStyle(ChatFormatting.RED));
            playDenied(level, pos);
            AuditLogger.log(AuditLogger.Event.ACCESS_DENIED, player.getName().getString(), AccessManager.describe(level, pos), "card level " + card.level());
            return InteractionResult.FAIL;
        }
        player.sendOverlayMessage(Component.translatable("message.securemod.access_granted").withStyle(ChatFormatting.GREEN));
        activate(state, level, pos, player);
        return InteractionResult.SUCCESS;
    }
}
