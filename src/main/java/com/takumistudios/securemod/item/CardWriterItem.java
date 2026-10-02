package com.takumistudios.securemod.item;

import com.takumistudios.securemod.registry.ModComponents;
import com.takumistudios.securemod.security.AuditLogger;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Card Writer: con el escritor en una mano y una Keycard en la otra, clic derecho al aire vincula la tarjeta
 * a tu nombre. La tarjeta abrirá tus lectores cuyo nivel requerido sea menor o igual al de la tarjeta.
 */
public class CardWriterItem extends Item {
    public CardWriterItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        InteractionHand otherHand = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack card = player.getItemInHand(otherHand);
        if (!(card.getItem() instanceof KeycardItem keycard)) {
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.sendOverlayMessage(Component.translatable("message.securemod.writer_need_card").withStyle(ChatFormatting.YELLOW));
            }
            return InteractionResult.FAIL;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            card.set(ModComponents.CARD_LINK, new ModComponents.CardLink(player.getUUID(), player.getName().getString()));
            level.playSound(null, player.blockPosition(), SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundSource.PLAYERS, 1.0F, 1.0F);
            serverPlayer.sendOverlayMessage(Component.translatable("message.securemod.card_linked", keycard.level()).withStyle(ChatFormatting.GREEN));
            AuditLogger.log(AuditLogger.Event.CARD_LINKED, player.getName().getString(), "-", "level " + keycard.level());
        }
        return InteractionResult.SUCCESS;
    }
}
