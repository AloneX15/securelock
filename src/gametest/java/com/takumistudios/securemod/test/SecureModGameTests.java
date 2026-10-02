package com.takumistudios.securemod.test;

import com.takumistudios.securemod.event.BlockBreakHandler;
import com.takumistudios.securemod.event.UseBlockHandler;
import com.takumistudios.securemod.lock.ChunkLocks;
import com.takumistudios.securemod.lock.LockData;
import com.takumistudios.securemod.lock.LockKind;
import com.takumistudios.securemod.lock.LockManager;
import com.takumistudios.securemod.registry.ModBlocks;
import com.mojang.serialization.DataResult;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * Batería de pruebas de bypass (PLAN, sección 13). Se ejecuta en la CI con {@code ./gradlew runGameTest}
 * en cada versión soportada. Las posiciones son relativas a la estructura de la prueba.
 */
public class SecureModGameTests {
    private static final UUID STRANGER_OWNER = UUID.fromString("00000000-0000-0000-0000-00000000beef");

    private static LockData padlock(UUID owner) {
        return LockData.create(owner, "owner", LockKind.PADLOCK, "minecraft:chest");
    }

    private static void lock(GameTestHelper helper, BlockPos relative, LockData data) {
        LockManager.put(helper.getLevel(), helper.absolutePos(relative), data);
    }

    private static void check(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            helper.fail(message);
        }
    }

    // ---------- Tolvas ----------

    @GameTest(maxTicks = 60)
    public void hopperCannotStealFromPadlockedChest(GameTestHelper helper) {
        BlockPos chest = new BlockPos(1, 2, 1);
        BlockPos hopper = new BlockPos(1, 1, 1);
        helper.setBlock(chest, Blocks.CHEST);
        helper.setBlock(hopper, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        Container container = (Container) helper.getLevel().getBlockEntity(helper.absolutePos(chest));
        container.setItem(0, new ItemStack(Items.DIAMOND, 5));
        lock(helper, chest, padlock(STRANGER_OWNER));
        helper.runAfterDelay(40, () -> {
            helper.assertContainerContains(chest, Items.DIAMOND);
            helper.assertContainerEmpty(hopper);
            helper.succeed();
        });
    }

    @GameTest(maxTicks = 60)
    public void hopperCannotFillPadlockedChest(GameTestHelper helper) {
        BlockPos hopper = new BlockPos(1, 2, 1);
        BlockPos chest = new BlockPos(1, 1, 1);
        helper.setBlock(chest, Blocks.CHEST);
        helper.setBlock(hopper, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        Container hopperContainer = (Container) helper.getLevel().getBlockEntity(helper.absolutePos(hopper));
        hopperContainer.setItem(0, new ItemStack(Items.DIRT, 1));
        lock(helper, chest, padlock(STRANGER_OWNER));
        helper.runAfterDelay(40, () -> {
            helper.assertContainerEmpty(chest);
            helper.assertContainerContains(hopper, Items.DIRT);
            helper.succeed();
        });
    }

    // ---------- Explosiones ----------

    @GameTest(maxTicks = 20)
    public void explosionSparesPadlockedChest(GameTestHelper helper) {
        BlockPos chest = new BlockPos(1, 1, 1);
        helper.setBlock(chest, Blocks.CHEST);
        lock(helper, chest, padlock(STRANGER_OWNER));
        BlockPos abs = helper.absolutePos(chest);
        helper.getLevel().explode(null, abs.getX() + 1.5, abs.getY() + 0.5, abs.getZ() + 0.5, 4.0F, Level.ExplosionInteraction.TNT);
        helper.assertBlockPresent(Blocks.CHEST, chest);
        helper.succeed();
    }

    @GameTest(maxTicks = 20)
    public void explosionSparesSupportOfPadlockedDoor(GameTestHelper helper) {
        BlockPos support = new BlockPos(1, 1, 1);
        BlockPos door = new BlockPos(1, 2, 1);
        helper.setBlock(support, Blocks.DIRT);
        placeDoor(helper, door, Blocks.OAK_DOOR.defaultBlockState());
        lock(helper, door, LockData.create(STRANGER_OWNER, "owner", LockKind.PADLOCK, "minecraft:oak_door"));
        BlockPos abs = helper.absolutePos(support);
        helper.getLevel().explode(null, abs.getX() + 1.5, abs.getY() + 0.5, abs.getZ() + 0.5, 3.0F, Level.ExplosionInteraction.TNT);
        helper.assertBlockPresent(Blocks.DIRT, support);
        helper.assertBlockPresent(Blocks.OAK_DOOR, door);
        helper.succeed();
    }

    // ---------- Pistones ----------

    @GameTest(maxTicks = 40)
    public void pistonCannotDestroyPadlockedDoor(GameTestHelper helper) {
        BlockPos piston = new BlockPos(0, 1, 1);
        BlockPos door = new BlockPos(1, 1, 1);
        helper.setBlock(new BlockPos(1, 0, 1), Blocks.STONE);
        placeDoor(helper, door, Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.EAST));
        lock(helper, door, LockData.create(STRANGER_OWNER, "owner", LockKind.PADLOCK, "minecraft:oak_door"));
        helper.setBlock(piston, Blocks.PISTON.defaultBlockState().setValue(net.minecraft.world.level.block.piston.PistonBaseBlock.FACING, Direction.EAST));
        helper.setBlock(new BlockPos(0, 2, 1), Blocks.REDSTONE_BLOCK);
        helper.runAfterDelay(10, () -> {
            helper.assertBlockPresent(Blocks.OAK_DOOR, door);
            helper.succeed();
        });
    }

    // ---------- Jugadores ----------

    @GameTest
    public void strangerCannotOpenButOwnerCan(GameTestHelper helper) {
        BlockPos chest = new BlockPos(1, 1, 1);
        helper.setBlock(chest, Blocks.CHEST);
        ServerPlayer owner = helper.makeMockServerPlayerInLevel();
        ServerPlayer stranger = helper.makeMockServerPlayerInLevel();
        lock(helper, chest, padlock(owner.getUUID()));
        BlockPos abs = helper.absolutePos(chest);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false);
        check(helper, UseBlockHandler.onUseBlock(stranger, helper.getLevel(), InteractionHand.MAIN_HAND, hit) == InteractionResult.FAIL,
                "un jugador ajeno abrió el cofre");
        stranger.setShiftKeyDown(true);
        check(helper, UseBlockHandler.onUseBlock(stranger, helper.getLevel(), InteractionHand.MAIN_HAND, hit) == InteractionResult.FAIL,
                "un jugador ajeno agachado abrió el cofre");
        check(helper, UseBlockHandler.onUseBlock(owner, helper.getLevel(), InteractionHand.MAIN_HAND, hit) == InteractionResult.PASS,
                "el propietario no pudo abrir su cofre");
        helper.succeed();
    }

    @GameTest
    public void spectatorCannotPeekInside(GameTestHelper helper) {
        BlockPos chest = new BlockPos(1, 1, 1);
        helper.setBlock(chest, Blocks.CHEST);
        lock(helper, chest, padlock(STRANGER_OWNER));
        ServerPlayer spectator = helper.makeMockServerPlayerInLevel();
        spectator.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
        BlockPos abs = helper.absolutePos(chest);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false);
        check(helper, UseBlockHandler.onUseBlock(spectator, helper.getLevel(), InteractionHand.MAIN_HAND, hit) == InteractionResult.FAIL,
                "un espectador pudo ver el contenido del cofre");
        helper.succeed();
    }

    @GameTest
    public void creativePickBlockDoesNotCopyContents(GameTestHelper helper) {
        BlockPos chest = new BlockPos(1, 1, 1);
        helper.setBlock(chest, Blocks.CHEST);
        lock(helper, chest, padlock(STRANGER_OWNER));
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        BlockPos abs = helper.absolutePos(chest);
        ItemStack picked = BlockBreakHandler.onPickBlock(player, abs, helper.getLevel().getBlockState(abs), true);
        check(helper, picked != null && picked.is(Items.CHEST) && picked.getComponentsPatch().isEmpty(),
                "el pick block con datos copió el contenido de un cofre protegido");
        check(helper, BlockBreakHandler.onPickBlock(player, abs, helper.getLevel().getBlockState(abs), false) == null,
                "el pick block normal no debe cambiar");
        helper.succeed();
    }

    @GameTest
    public void strangerCannotBreakButOwnerCan(GameTestHelper helper) {
        BlockPos chest = new BlockPos(1, 1, 1);
        helper.setBlock(chest, Blocks.CHEST);
        ServerPlayer owner = helper.makeMockServerPlayerInLevel();
        ServerPlayer stranger = helper.makeMockServerPlayerInLevel();
        lock(helper, chest, padlock(owner.getUUID()));
        BlockPos abs = helper.absolutePos(chest);
        BlockState state = helper.getLevel().getBlockState(abs);
        check(helper, !BlockBreakHandler.beforeBreak(helper.getLevel(), stranger, abs, state, null), "un jugador ajeno rompió el cofre");
        check(helper, BlockBreakHandler.beforeBreak(helper.getLevel(), owner, abs, state, null), "el propietario no pudo romper su cofre");
        helper.succeed();
    }

    @GameTest
    public void doubleChestBothHalvesProtected(GameTestHelper helper) {
        BlockPos left = new BlockPos(1, 1, 1);
        BlockPos right = new BlockPos(2, 1, 1);
        BlockState base = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH);
        helper.setBlock(left, base.setValue(ChestBlock.TYPE, ChestType.LEFT));
        helper.setBlock(right, base.setValue(ChestBlock.TYPE, ChestType.RIGHT));
        lock(helper, left, padlock(STRANGER_OWNER));
        check(helper, LockManager.isProtected(helper.getLevel(), helper.absolutePos(right)), "la otra mitad del cofre doble no está protegida");
        helper.succeed();
    }

    // ---------- Bloques propios ----------

    @GameTest(maxTicks = 40)
    public void reinforcedDoorIgnoresExternalRedstone(GameTestHelper helper) {
        BlockPos door = new BlockPos(1, 1, 1);
        helper.setBlock(new BlockPos(1, 0, 1), Blocks.STONE);
        placeDoor(helper, door, ModBlocks.REINFORCED_IRON_DOOR.defaultBlockState());
        lock(helper, door, LockData.create(STRANGER_OWNER, "owner", LockKind.BLOCK, "securemod:reinforced_iron_door"));
        helper.setBlock(new BlockPos(2, 1, 1), Blocks.REDSTONE_BLOCK);
        helper.runAfterDelay(5, () -> {
            check(helper, !helper.getBlockState(door).getValue(DoorBlock.OPEN), "la puerta reforzada se abrió con redstone externa");
            helper.succeed();
        });
    }

    @GameTest(maxTicks = 40)
    public void reinforcedDoorOpensWithOwnersKeypad(GameTestHelper helper) {
        BlockPos door = new BlockPos(1, 1, 1);
        BlockPos keypad = new BlockPos(2, 1, 1);
        helper.setBlock(new BlockPos(1, 0, 1), Blocks.STONE);
        helper.setBlock(new BlockPos(3, 1, 1), Blocks.STONE);
        placeDoor(helper, door, ModBlocks.REINFORCED_IRON_DOOR.defaultBlockState());
        lock(helper, door, LockData.create(STRANGER_OWNER, "owner", LockKind.BLOCK, "securemod:reinforced_iron_door"));
        BlockState keypadState = ModBlocks.KEYPAD.defaultBlockState()
                .setValue(ButtonBlock.FACE, AttachFace.WALL).setValue(ButtonBlock.FACING, Direction.WEST);
        helper.setBlock(keypad, keypadState);
        lock(helper, keypad, LockData.create(STRANGER_OWNER, "owner", LockKind.BLOCK, "securemod:keypad"));
        ((com.takumistudios.securemod.block.SecureEmitterBlock) ModBlocks.KEYPAD).activate(helper.getBlockState(keypad), helper.getLevel(), helper.absolutePos(keypad), null);
        helper.runAfterDelay(3, () -> {
            check(helper, helper.getBlockState(door).getValue(DoorBlock.OPEN), "el teclado del propietario no abrió la puerta");
            helper.succeed();
        });
    }

    @GameTest
    public void reinforcedBlocksAreImmovableAndBlastProof(GameTestHelper helper) {
        BlockState state = ModBlocks.PASSCODE_CHEST.defaultBlockState();
        check(helper, state.getBlock().getExplosionResistance() >= ModBlocks.RESISTANCE, "resistencia a explosiones insuficiente");
        check(helper, state.getDestroySpeed(helper.getLevel(), BlockPos.ZERO) < 0, "el bloque propio no es irrompible en survival");
        helper.succeed();
    }

    // ---------- Administración ----------

    @GameTest
    public void purgeThenRollbackRestoresProtection(GameTestHelper helper) {
        UUID owner = UUID.randomUUID();
        BlockPos chest = new BlockPos(1, 1, 1);
        helper.setBlock(chest, Blocks.CHEST);
        lock(helper, chest, padlock(owner).withPasscode("2468"));
        net.minecraft.server.MinecraftServer server = helper.getLevel().getServer();
        int removed = com.takumistudios.securemod.data.LockAdmin.purge(server, owner);
        check(helper, removed == 1 && !LockManager.isProtected(helper.getLevel(), helper.absolutePos(chest)), "la purga no quitó la protección");
        java.util.List<String> backups = com.takumistudios.securemod.data.LockAdmin.backups();
        check(helper, !backups.isEmpty(), "la purga no creó una copia de seguridad");
        try {
            com.takumistudios.securemod.data.LockAdmin.RollbackResult result =
                    com.takumistudios.securemod.data.LockAdmin.rollback(server, backups.getLast());
            check(helper, result.restored() >= 1, "el rollback no restauró nada");
        } catch (java.io.IOException e) {
            helper.fail("rollback falló: " + e.getMessage());
        }
        LockManager.Locked restored = LockManager.find(helper.getLevel(), helper.absolutePos(chest));
        check(helper, restored != null && restored.lock().isOwner(owner) && restored.lock().checkPasscode("2468"),
                "el rollback no restauró propietario y código");
        helper.succeed();
    }

    // ---------- Persistencia ----------

    @GameTest
    public void lockDataSurvivesSerialization(GameTestHelper helper) {
        LockData original = padlock(STRANGER_OWNER).withPasscode("4821").withAllowed(UUID.randomUUID(), "friend", true).withCardLevel(3);
        ChunkLocks locks = ChunkLocks.EMPTY.with(new BlockPos(10, 64, -3), original);
        DataResult<Tag> encoded = ChunkLocks.CODEC.encodeStart(NbtOps.INSTANCE, locks);
        ChunkLocks decoded = ChunkLocks.CODEC.parse(NbtOps.INSTANCE, encoded.getOrThrow()).getOrThrow();
        LockData copy = decoded.get(new BlockPos(10, 64, -3));
        check(helper, copy != null && copy.equals(original), "los datos de protección no sobreviven al guardado");
        check(helper, copy.checkPasscode("4821") && !copy.checkPasscode("0000"), "el código no se conserva");
        helper.succeed();
    }

    @GameTest
    public void futureDataVersionIsLockedForEveryone(GameTestHelper helper) {
        LockData future = new LockData(STRANGER_OWNER, "owner", com.takumistudios.securemod.security.AccessMode.PUBLIC, java.util.Map.of(), java.util.Map.of(),
                java.util.Optional.empty(), java.util.Optional.empty(), 0, LockKind.PADLOCK, "minecraft:chest", 0L, LockData.CURRENT_DATA_VERSION + 1);
        BlockPos chest = new BlockPos(1, 1, 1);
        helper.setBlock(chest, Blocks.CHEST);
        lock(helper, chest, future);
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        BlockPos abs = helper.absolutePos(chest);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs, false);
        check(helper, UseBlockHandler.onUseBlock(player, helper.getLevel(), InteractionHand.MAIN_HAND, hit) == InteractionResult.FAIL,
                "un bloque con datos de una versión futura no quedó cerrado");
        helper.succeed();
    }

    // ---------- Datos (recetas y loot tables cargan en esta versión) ----------

    @GameTest
    public void recipesAndLootTablesLoad(GameTestHelper helper) {
        net.minecraft.server.MinecraftServer server = helper.getLevel().getServer();
        for (String recipe : java.util.List.of("reinforced_iron_door", "reinforced_oak_door", "reinforced_iron_trapdoor", "reinforced_oak_fence_gate",
                "passcode_chest", "passcode_barrel", "keypad", "card_reader", "biometric_scanner", "padlock", "universal_modifier",
                "universal_block_remover", "card_writer", "keycard_lv1", "keycard_lv2", "keycard_lv3", "keycard_lv4", "keycard_lv5")) {
            net.minecraft.resources.ResourceKey<net.minecraft.world.item.crafting.Recipe<?>> key = net.minecraft.resources.ResourceKey.create(
                    net.minecraft.core.registries.Registries.RECIPE, net.minecraft.resources.Identifier.fromNamespaceAndPath("securemod", recipe));
            check(helper, server.getRecipeManager().byKey(key).isPresent(), "receta no cargada: " + recipe);
        }
        for (net.minecraft.world.level.block.Block block : java.util.List.of(ModBlocks.REINFORCED_IRON_DOOR, ModBlocks.PASSCODE_CHEST, ModBlocks.KEYPAD,
                ModBlocks.CARD_READER, ModBlocks.BIOMETRIC_SCANNER, ModBlocks.PASSCODE_BARREL, ModBlocks.REINFORCED_OAK_FENCE_GATE)) {
            net.minecraft.world.level.storage.loot.LootTable table = server.reloadableRegistries().getLootTable(block.getLootTable().orElseThrow());
            check(helper, table != net.minecraft.world.level.storage.loot.LootTable.EMPTY, "loot table no cargada: " + block);
        }
        check(helper, !com.takumistudios.securemod.lock.LockManager.isLockable(ModBlocks.PASSCODE_CHEST.defaultBlockState()), "un bloque propio no debe aceptar candado");
        check(helper, Blocks.CHEST.defaultBlockState().is(com.takumistudios.securemod.registry.ModTags.LOCKABLE), "el tag securemod:lockable no cargó");
        check(helper, !Blocks.ENDER_CHEST.defaultBlockState().is(com.takumistudios.securemod.registry.ModTags.LOCKABLE)
                || Blocks.ENDER_CHEST.defaultBlockState().is(com.takumistudios.securemod.registry.ModTags.NEVER_LOCK), "el cofre de ender no debe poder bloquearse");
        helper.succeed();
    }

    // ---------- Mixins ----------

    @GameTest
    public void mixinsAreConnected(GameTestHelper helper) {
        // Carga las clases destino (los mixins se aplican al cargar la clase)
        for (String target : java.util.List.of("net.minecraft.world.level.block.piston.PistonStructureResolver", "net.minecraft.world.level.ServerExplosion",
                "net.minecraft.world.entity.ai.behavior.TransportItemsBetweenContainers")) {
            try {
                Class.forName(target);
            } catch (ClassNotFoundException e) {
                helper.fail("no existe " + target);
            }
        }
        java.util.Map<String, Boolean> status = com.takumistudios.securemod.compat.SecureModMixinPlugin.status();
        for (String mixin : java.util.List.of("HopperBlockEntityMixin", "PistonStructureResolverMixin", "FireBlockMixin", "ServerExplosionMixin",
                "ComparatorBlockMixin", "DoorBlockMixin", "WitherBossMixin", "EnderDragonMixin", "TransportItemsBetweenContainersMixin")) {
            check(helper, Boolean.TRUE.equals(status.get(mixin)), "mixin no conectado o no verificado: " + mixin + " -> " + status);
        }
        helper.succeed();
    }

    // ---------- Rendimiento ----------

    @GameTest(maxTicks = 100)
    public void thousandProtectedBlocksLookupIsFast(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(0, 1, 0));
        for (int i = 0; i < 1000; i++) {
            BlockPos pos = origin.offset(i % 10, i / 100, (i / 10) % 10);
            level.setBlock(pos, Blocks.CHEST.defaultBlockState(), 2);
            LockManager.put(level, pos, padlock(STRANGER_OWNER));
        }
        long start = System.nanoTime();
        int found = 0;
        for (int round = 0; round < 100; round++) {
            for (int i = 0; i < 1000; i++) {
                if (LockManager.find(level, origin.offset(i % 10, i / 100, (i / 10) % 10)) != null) {
                    found++;
                }
            }
        }
        long micros = (System.nanoTime() - start) / 1000;
        check(helper, found == 100_000, "no se encontraron todas las protecciones: " + found);
        // 100 000 consultas: el objetivo es muy inferior a 1 s incluso en la CI
        check(helper, micros < 1_000_000, "consultas demasiado lentas: " + micros + " µs");
        helper.succeed();
    }

    private static void placeDoor(GameTestHelper helper, BlockPos lower, BlockState base) {
        helper.setBlock(lower, base.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        helper.setBlock(lower.above(), base.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
    }
}
