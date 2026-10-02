package com.alonex15.securelock.registry;

import com.alonex15.securelock.SecureLock;
import com.alonex15.securelock.block.BiometricScannerBlock;
import com.alonex15.securelock.block.CardReaderBlock;
import com.alonex15.securelock.block.KeypadBlock;
import com.alonex15.securelock.block.PasscodeBarrelBlock;
import com.alonex15.securelock.block.PasscodeChestBlock;
import com.alonex15.securelock.block.ReinforcedDoorBlock;
import com.alonex15.securelock.block.ReinforcedFenceGateBlock;
import com.alonex15.securelock.block.ReinforcedTrapdoorBlock;
import net.fabricmc.fabric.api.object.builder.v1.block.type.BlockSetTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
//? if >=26.2 {
import net.minecraft.world.level.block.entity.BlockEntityTypes;
//?}
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import java.util.function.Function;

/**
 * Bloques propios. Todos extienden la clase vanilla equivalente ({@code DoorBlock}, {@code ChestBlock}...),
 * así las comprobaciones {@code instanceof} de otros mods siguen funcionando.
 * Dureza -1 (irrompible en survival), resistencia 3 600 000 y {@code PushReaction.BLOCK}.
 */
public final class ModBlocks {
    public static final float RESISTANCE = 3_600_000.0F;

    /** Como el roble, pero los mobs (aldeanos, zombis) no la consideran una puerta de madera: ni la abren ni la rompen. */
    public static final BlockSetType REINFORCED_OAK_SET = BlockSetTypeBuilder.copyOf(BlockSetType.OAK)
            .openableByHand(false)
            .openableByWindCharge(false)
            .buttonActivatedByArrows(false)
            .register(Identifier.fromNamespaceAndPath(SecureLock.MOD_ID, "reinforced_oak"));

    public static final Block REINFORCED_IRON_DOOR = register("reinforced_iron_door",
            p -> new ReinforcedDoorBlock(BlockSetType.IRON, p, () -> Blocks.IRON_DOOR),
            secure(MapColor.METAL).noOcclusion());
    public static final Block REINFORCED_OAK_DOOR = register("reinforced_oak_door",
            p -> new ReinforcedDoorBlock(REINFORCED_OAK_SET, p, () -> Blocks.OAK_DOOR),
            secure(MapColor.WOOD).noOcclusion().instrument(NoteBlockInstrument.BASS));
    public static final Block REINFORCED_IRON_TRAPDOOR = register("reinforced_iron_trapdoor",
            p -> new ReinforcedTrapdoorBlock(BlockSetType.IRON, p, () -> Blocks.IRON_TRAPDOOR),
            secure(MapColor.METAL).noOcclusion().isValidSpawn(Blocks::never));
    public static final Block REINFORCED_OAK_FENCE_GATE = register("reinforced_oak_fence_gate",
            p -> new ReinforcedFenceGateBlock(WoodType.OAK, p, () -> Blocks.OAK_FENCE_GATE),
            secure(MapColor.WOOD).forceSolidOn().instrument(NoteBlockInstrument.BASS));
    public static final Block PASSCODE_CHEST = register("passcode_chest",
            //? if >=26.2 {
            p -> new PasscodeChestBlock(() -> BlockEntityTypes.CHEST, p),
            //?} else {
            /*p -> new PasscodeChestBlock(() -> BlockEntityType.CHEST, p),
            *///?}
            secure(MapColor.METAL).instrument(NoteBlockInstrument.BASS).sound(SoundType.METAL));
    public static final Block PASSCODE_BARREL = register("passcode_barrel",
            PasscodeBarrelBlock::new,
            secure(MapColor.METAL).instrument(NoteBlockInstrument.BASS).sound(SoundType.METAL));
    public static final Block KEYPAD = register("keypad", KeypadBlock::new,
            secure(MapColor.METAL).noCollision().sound(SoundType.METAL));
    public static final Block CARD_READER = register("card_reader", CardReaderBlock::new,
            secure(MapColor.METAL).noCollision().sound(SoundType.METAL));
    public static final Block BIOMETRIC_SCANNER = register("biometric_scanner", BiometricScannerBlock::new,
            secure(MapColor.METAL).noCollision().sound(SoundType.METAL));

    private ModBlocks() {
    }

    private static BlockBehaviour.Properties secure(MapColor color) {
        return BlockBehaviour.Properties.of()
                .mapColor(color)
                .strength(-1.0F, RESISTANCE)
                //? if >=26.3 {
                .pushReaction(PushReaction.IMMOVEABLE)
                //?} else {
                /*.pushReaction(PushReaction.BLOCK)
                *///?}
                .sound(SoundType.METAL);
    }

    private static Block register(String name, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(SecureLock.MOD_ID, name));
        return Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(properties.setId(key)));
    }

    public static void init() {
        // Los cofres y barriles con contraseña usan los BlockEntity vanilla (compatibilidad máxima)
        //? if >=26.2 {
        BlockEntityTypes.CHEST.addValidBlock(PASSCODE_CHEST);
        BlockEntityTypes.BARREL.addValidBlock(PASSCODE_BARREL);
        //?} else {
        /*BlockEntityType.CHEST.addValidBlock(PASSCODE_CHEST);
        BlockEntityType.BARREL.addValidBlock(PASSCODE_BARREL);
        *///?}
    }
}
