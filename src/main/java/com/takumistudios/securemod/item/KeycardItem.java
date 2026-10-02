package com.takumistudios.securemod.item;

import com.takumistudios.securemod.block.CardReaderBlock;
import com.takumistudios.securemod.lock.LockManager;
import com.takumistudios.securemod.registry.ModComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/** Tarjeta de acceso de nivel 1–5. Se vincula a un propietario con el Card Writer. */
public class KeycardItem extends Item implements SecureTool {
    private final int level;

    public KeycardItem(int level, Item.Properties properties) {
        super(properties);
        this.level = level;
    }

    public int level() {
        return level;
    }

    @Override
    public InteractionResult useOnBlock(ServerPlayer player, ServerLevel serverLevel, BlockPos pos, BlockState state, ItemStack stack,
                                        InteractionHand hand, LockManager.@Nullable Locked locked) {
        if (state.getBlock() instanceof CardReaderBlock reader) {
            return reader.swipe(state, serverLevel, pos, player, stack);
        }
        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack itemStack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag tooltipFlag) {
        builder.accept(Component.translatable("tooltip.securemod.card_level", level).withStyle(ChatFormatting.GRAY));
        ModComponents.CardLink link = itemStack.get(ModComponents.CARD_LINK);
        if (link == null) {
            builder.accept(Component.translatable("tooltip.securemod.card_unlinked").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            builder.accept(Component.translatable("tooltip.securemod.card_owner", link.ownerName()).withStyle(ChatFormatting.AQUA));
        }
    }
}
