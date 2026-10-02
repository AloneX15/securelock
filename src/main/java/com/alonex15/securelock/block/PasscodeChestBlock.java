package com.alonex15.securelock.block;

import com.alonex15.securelock.config.SecureLockConfig;
import com.alonex15.securelock.lock.LockManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Cofre con contraseña (simple y doble). Usa el {@code ChestBlockEntity} vanilla, así que los mods de
 * almacenamiento, ordenación y renderizado lo reconocen. Solo se une con cofres del mismo propietario.
 */
public class PasscodeChestBlock extends ChestBlock implements SecureLockBlock {
    public PasscodeChestBlock(Supplier<BlockEntityType<? extends ChestBlockEntity>> blockEntityType, BlockBehaviour.Properties properties) {
        super(blockEntityType, SoundEvents.CHEST_OPEN, SoundEvents.CHEST_CLOSE, properties);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        if (context.getPlayer() != null && OwnedBlocks.isAtLimit(context.getPlayer())) {
            return null;
        }
        BlockState state = super.getStateForPlacement(context);
        if (state == null || state.getValue(TYPE) == ChestType.SINGLE || !(context.getLevel() instanceof ServerLevel level)) {
            return state;
        }
        // No unirse a la mitad de otro propietario
        Direction toOther = getConnectedDirection(state);
        LockManager.Locked other = LockManager.getAt(level, context.getClickedPos().relative(toOther));
        if (other == null || context.getPlayer() == null || !other.lock().isOwner(context.getPlayer().getUUID())) {
            return state.setValue(TYPE, ChestType.SINGLE);
        }
        return state;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack itemStack) {
        super.setPlacedBy(level, pos, state, by, itemStack);
        OwnedBlocks.claim(level, pos, state, by);
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        if (SecureLockConfig.get().comparatorOutputHidden && LockManager.isProtected(level, pos)) {
            return 0;
        }
        return super.getAnalogOutputSignal(state, level, pos, direction);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        OwnedBlocks.onRemoved(level, pos, state);
    }

    @Override
    public boolean opensConfigOnPlace() {
        return true;
    }

    @Override
    public @Nullable BlockState toVanilla(ServerLevel level, BlockPos pos, BlockState state) {
        return Blocks.CHEST.withPropertiesOf(state).setValue(TYPE, ChestType.SINGLE);
    }
}
