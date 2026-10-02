package com.alonex15.securelock.block;

import com.alonex15.securelock.config.SecureLockConfig;
import com.alonex15.securelock.item.KeycardItem;
import com.alonex15.securelock.lock.LockManager;
import com.alonex15.securelock.registry.ModComponents;
import com.alonex15.securelock.security.AuditLogger;
import com.alonex15.securelock.access.AccessManager;
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
        return SecureLockConfig.get().cardReaderSignalTicks;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendOverlayMessage(Component.translatable("message.securelock.insert_card").withStyle(ChatFormatting.YELLOW));
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
            player.sendOverlayMessage(Component.translatable("message.securelock.card_unlinked").withStyle(ChatFormatting.RED));
            playDenied(level, pos);
            return InteractionResult.FAIL;
        }
        if (!link.owner().equals(locked.lock().owner()) || card.level() < required || locked.lock().isCorrupt()) {
            player.sendOverlayMessage(Component.translatable("message.securelock.card_denied", required).withStyle(ChatFormatting.RED));
            playDenied(level, pos);
            AuditLogger.log(AuditLogger.Event.ACCESS_DENIED, player.getName().getString(), AccessManager.describe(level, pos), "card level " + card.level());
            return InteractionResult.FAIL;
        }
        player.sendOverlayMessage(Component.translatable("message.securelock.access_granted").withStyle(ChatFormatting.GREEN));
        activate(state, level, pos, player);
        return InteractionResult.SUCCESS;
    }
}
