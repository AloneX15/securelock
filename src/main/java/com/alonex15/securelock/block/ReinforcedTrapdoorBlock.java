package com.alonex15.securelock.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

/** Trampilla reforzada: mismas reglas que la puerta reforzada. */
public class ReinforcedTrapdoorBlock extends TrapDoorBlock implements SecureLockBlock {
    private final Supplier<Block> vanilla;

    public ReinforcedTrapdoorBlock(BlockSetType type, BlockBehaviour.Properties properties, Supplier<Block> vanilla) {
        super(type, properties);
        this.vanilla = vanilla;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        if (context.getPlayer() != null && OwnedBlocks.isAtLimit(context.getPlayer())) {
            return null;
        }
        BlockState state = super.getStateForPlacement(context);
        // Nace cerrada aunque haya redstone externa al lado
        return state == null ? null : state.setValue(POWERED, false).setValue(OPEN, false);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack itemStack) {
        super.setPlacedBy(level, pos, state, by, itemStack);
        OwnedBlocks.claim(level, pos, state, by);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        BlockState updated = state.cycle(OPEN);
        level.setBlock(pos, updated, Block.UPDATE_CLIENTS);
        if (updated.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        this.playSound(player, level, pos, updated.getValue(OPEN));
        level.gameEvent(player, updated.getValue(OPEN) ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
        if (level.isClientSide()) {
            return;
        }
        boolean signal = RedstoneGate.hasAuthorizedSignal(level, pos, pos);
        if (signal != state.getValue(POWERED)) {
            if (state.getValue(OPEN) != signal) {
                state = state.setValue(OPEN, signal);
                this.playSound(null, level, pos, signal);
            }
            level.setBlock(pos, state.setValue(POWERED, signal), Block.UPDATE_CLIENTS);
            if (state.getValue(WATERLOGGED)) {
                level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
            }
        }
    }

    @Override
    protected void onExplosionHit(BlockState state, ServerLevel level, BlockPos pos, Explosion explosion, BiConsumer<ItemStack, BlockPos> onHit) {
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        OwnedBlocks.onRemoved(level, pos, state);
    }

    @Override
    public @Nullable BlockState toVanilla(ServerLevel level, BlockPos pos, BlockState state) {
        return vanilla.get().withPropertiesOf(state);
    }
}
