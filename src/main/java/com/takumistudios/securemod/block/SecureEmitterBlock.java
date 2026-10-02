package com.takumistudios.securemod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Base de los emisores de redstone (teclado, lector de tarjetas, escáner biométrico).
 * Se colocan en paredes, suelo o techo como un botón y emiten señal durante N ticks al autorizar.
 * Sin BlockEntity y sin ticks: solo un tick programado al desactivarse.
 */
public abstract class SecureEmitterBlock extends ButtonBlock implements SecureModBlock, RedstoneGate.SecureEmitter {
    private static final Map<AttachFace, Map<Direction, VoxelShape>> SHAPES = Shapes.rotateAttachFace(Block.boxZ(10.0, 12.0, 14.0, 16.0));

    protected SecureEmitterBlock(BlockBehaviour.Properties properties) {
        super(BlockSetType.IRON, 20, properties);
    }

    /** Dirección hacia el bloque al que está pegado (pared, suelo o techo). */
    public static Direction attachedTo(BlockState state) {
        return switch (state.getValue(FACE)) {
            case FLOOR -> Direction.DOWN;
            case CEILING -> Direction.UP;
            case WALL -> state.getValue(FACING).getOpposite();
        };
    }

    /** Ticks que emite señal (configurable). */
    protected abstract int signalTicks();

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACE)).get(state.getValue(FACING));
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        if (context.getPlayer() != null && OwnedBlocks.isAtLimit(context.getPlayer())) {
            return null;
        }
        return super.getStateForPlacement(context);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack itemStack) {
        super.setPlacedBy(level, pos, state, by, itemStack);
        OwnedBlocks.claim(level, pos, state, by);
    }

    /** Emite señal de redstone durante {@link #signalTicks()} ticks. */
    public void activate(BlockState state, Level level, BlockPos pos, @Nullable Player player) {
        if (state.getValue(POWERED)) {
            return;
        }
        level.setBlockAndUpdate(pos, state.setValue(POWERED, true));
        Direction front = getConnectedDirection(state).getOpposite();
        level.updateNeighborsAt(pos, this, null);
        level.updateNeighborsAt(pos.relative(front), this, null);
        level.scheduleTick(pos, this, signalTicks());
        level.playSound(null, pos, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 0.6F, 1.6F);
        level.gameEvent(player, GameEvent.BLOCK_ACTIVATE, pos);
    }

    /** Sonido y partículas de denegación. */
    public static void playDenied(Level level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.BLOCKS, 0.8F, 0.5F);
    }

    @Override
    public void press(BlockState state, Level level, BlockPos pos, @Nullable Player player) {
        // Solo se activa con activate(): nadie puede "pulsarlo" como un botón normal
    }

    @Override
    protected void onExplosionHit(BlockState state, ServerLevel level, BlockPos pos, Explosion explosion, BiConsumer<ItemStack, BlockPos> onHit) {
        // Las explosiones y cargas de viento no lo activan
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        OwnedBlocks.onRemoved(level, pos, state);
    }

    @Override
    public boolean isSelfGuarded() {
        return true;
    }

    @Override
    public @Nullable BlockState toVanilla(ServerLevel level, BlockPos pos, BlockState state) {
        return null;
    }
}
