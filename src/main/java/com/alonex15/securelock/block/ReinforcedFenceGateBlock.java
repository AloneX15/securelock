package com.alonex15.securelock.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.redstone.Orientation;
import org.jspecify.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

/** Puerta de valla reforzada: mismas reglas que la puerta reforzada. */
public class ReinforcedFenceGateBlock extends FenceGateBlock implements SecureLockBlock {
    private final WoodType woodType;
    private final Supplier<Block> vanilla;

    public ReinforcedFenceGateBlock(WoodType type, BlockBehaviour.Properties properties, Supplier<Block> vanilla) {
        super(type, properties);
        this.woodType = type;
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
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
        if (level.isClientSide()) {
            return;
        }
        boolean hasPower = RedstoneGate.hasAuthorizedSignal(level, pos, pos);
        if (state.getValue(POWERED) != hasPower) {
            level.setBlock(pos, state.setValue(POWERED, hasPower).setValue(OPEN, hasPower), Block.UPDATE_CLIENTS);
            if (state.getValue(OPEN) != hasPower) {
                level.playSound(null, pos, hasPower ? woodType.fenceGateOpen() : woodType.fenceGateClose(), SoundSource.BLOCKS, 1.0F,
                        level.getRandom().nextFloat() * 0.1F + 0.9F);
                level.gameEvent(null, hasPower ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);
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
