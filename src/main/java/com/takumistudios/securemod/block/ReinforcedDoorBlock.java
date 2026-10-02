package com.takumistudios.securemod.block;

import com.takumistudios.securemod.lock.LockManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * Puerta reforzada: solo la abren el propietario y los permitidos (incluso la de hierro, con la mano).
 * Ignora la redstone externa, no cae si se rompe el bloque de soporte y es irrompible para los demás.
 */
public class ReinforcedDoorBlock extends DoorBlock implements SecureModBlock {
    private final Supplier<Block> vanilla;

    public ReinforcedDoorBlock(BlockSetType type, BlockBehaviour.Properties properties, Supplier<Block> vanilla) {
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
        // El permiso ya se comprobó en el manejador de clic derecho (servidor)
        this.setOpen(player, level, state, pos, !this.isOpen(state));
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
        if (level.isClientSide() || this.defaultBlockState().is(block)) {
            return;
        }
        BlockPos lower = state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
        boolean signal = RedstoneGate.hasAuthorizedSignal(level, lower, lower) || RedstoneGate.hasAuthorizedSignal(level, lower.above(), lower);
        if (signal != state.getValue(POWERED)) {
            if (signal != state.getValue(OPEN)) {
                this.setOpen(null, level, state, pos, signal);
                state = level.getBlockState(pos);
            }
            level.setBlock(pos, state.setValue(POWERED, signal).setValue(OPEN, signal), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        // Una puerta ya colocada y protegida no cae aunque se rompa el bloque de debajo
        if (state.getValue(HALF) == DoubleBlockHalf.LOWER && level instanceof ServerLevel serverLevel && LockManager.getAt(serverLevel, pos) != null) {
            return true;
        }
        return super.canSurvive(state, level, pos);
    }

    @Override
    protected void onExplosionHit(BlockState state, ServerLevel level, BlockPos pos, Explosion explosion, BiConsumer<ItemStack, BlockPos> onHit) {
        // Ni las cargas de viento la abren ni las explosiones la rompen
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        if (state.getValue(HALF) == DoubleBlockHalf.LOWER) {
            OwnedBlocks.onRemoved(level, pos, state);
        }
    }

    @Override
    public @Nullable BlockState toVanilla(ServerLevel level, BlockPos pos, BlockState state) {
        return vanilla.get().defaultBlockState()
                .setValue(FACING, state.getValue(FACING))
                .setValue(HINGE, state.getValue(HINGE))
                .setValue(OPEN, state.getValue(OPEN))
                .setValue(HALF, DoubleBlockHalf.LOWER);
    }
}
