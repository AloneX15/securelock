package com.takumistudios.securemod.test;

import com.takumistudios.securemod.client.screen.BlockConfigScreen;
import com.takumistudios.securemod.client.screen.KeypadScreen;
import com.takumistudios.securemod.network.Payloads;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;

import java.util.List;

/**
 * Prueba de cliente: crea un mundo, coloca todos los bloques de Secure Mod y saca capturas
 * de la escena y de las pantallas (teclado y configuración) para revisar modelos, texturas y GUIs.
 */
public class SecureModClientGameTest implements FabricClientGameTest {
    private static void waitForChunks(TestSingleplayerContext world) {
        //? if >=26.2 {
        world.getConnection().waitForChunksRender();
        //?} else {
        /*world.getClientLevel().waitForChunksRender();
        *///?}
    }

    @Override
    public void runTest(ClientGameTestContext context) {
        BlockPos[] chestPos = new BlockPos[1];
        net.fabricmc.fabric.api.client.gametest.v1.world.TestWorldSave save;
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            save = world.getWorldSave();
            waitForChunks(world);
            world.getServer().runCommand("time set noon");
            world.getServer().runCommand("weather clear");
            world.getServer().runCommand("tp @p 0.5 ~ 2.5 180 15");
            context.waitTicks(5);
            String[] commands = {
                    "fill ~-5 ~ ~-6 ~5 ~3 ~-6 minecraft:stone_bricks",
                    "fill ~-5 ~ ~-5 ~5 ~3 ~-1 minecraft:air",
                    "setblock ~-4 ~ ~-4 securemod:reinforced_iron_door[half=lower,facing=south]",
                    "setblock ~-4 ~1 ~-4 securemod:reinforced_iron_door[half=upper,facing=south]",
                    "setblock ~-3 ~ ~-4 securemod:reinforced_oak_door[half=lower,facing=south]",
                    "setblock ~-3 ~1 ~-4 securemod:reinforced_oak_door[half=upper,facing=south]",
                    "setblock ~-2 ~ ~-4 securemod:passcode_chest[facing=south]",
                    "setblock ~-1 ~ ~-4 securemod:passcode_barrel[facing=up]",
                    "setblock ~0 ~ ~-4 securemod:reinforced_oak_fence_gate[facing=south]",
                    "setblock ~1 ~ ~-4 securemod:reinforced_iron_trapdoor[half=bottom,facing=south]",
                    "setblock ~2 ~1 ~-5 securemod:keypad[face=wall,facing=south]",
                    "setblock ~3 ~1 ~-5 securemod:card_reader[face=wall,facing=south]",
                    "setblock ~4 ~1 ~-5 securemod:biometric_scanner[face=wall,facing=south,powered=true]",
                    "setblock ~3 ~ ~-4 securemod:keypad[face=floor,facing=south]"
            };
            for (String command : commands) {
                world.getServer().runCommand("execute at @p run " + command);
            }
            for (String item : List.of("padlock", "universal_modifier", "keycard_lv3", "card_writer", "universal_block_remover", "admin_tool", "keypad", "passcode_chest", "reinforced_iron_door")) {
                world.getServer().runCommand("give @p securemod:" + item);
            }
            context.waitTicks(40);
            waitForChunks(world);
            context.takeScreenshot("securemod_blocks");

            // Protege el cofre a nombre del jugador y lo mira: HUD propio (o tooltip de Jade si está instalado)
            world.getServer().runOnServer(server -> {
                net.minecraft.server.level.ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
                BlockPos chest = player.blockPosition().offset(-2, 0, -4);
                chestPos[0] = chest;
                com.takumistudios.securemod.lock.LockManager.put(player.level(), chest,
                        com.takumistudios.securemod.lock.LockData.create(player.getUUID(), player.getName().getString(),
                                com.takumistudios.securemod.lock.LockKind.BLOCK, "securemod:passcode_chest").withPasscode("1234"));
            });
            world.getServer().runCommand("execute as @p at @s run tp @s ~ ~ ~ 156.8 18");
            context.waitTicks(30);
            context.takeScreenshot("securemod_owner_hud");

            context.setScreen(() -> new KeypadScreen(BlockPos.ZERO, 4, 16));
            context.waitTicks(5);
            context.takeScreenshot("securemod_keypad");

            context.setScreen(() -> new BlockConfigScreen(new Payloads.OpenConfigS2C(BlockPos.ZERO, "block.securemod.passcode_chest", "TakumiStudios", "shared",
                    List.of("Friend1", "Friend2"), List.of("Griefer"), true, 0, false, true, 4, 16)));
            context.waitTicks(5);
            context.takeScreenshot("securemod_config");

            context.setScreen(() -> new BlockConfigScreen(new Payloads.OpenConfigS2C(BlockPos.ZERO, "block.securemod.card_reader", "TakumiStudios", "private",
                    List.of(), List.of(), false, 3, true, false, 4, 16)));
            context.waitTicks(5);
            context.takeScreenshot("securemod_config_reader");
            context.setScreen(() -> null);
        }

        // Reinicio: las protecciones (adjuntos del chunk) y el índice deben persistir
        try (TestSingleplayerContext reopened = save.open()) {
            waitForChunks(reopened);
            boolean persisted = reopened.getServer().computeOnServer(server -> {
                com.takumistudios.securemod.lock.LockManager.Locked locked =
                        com.takumistudios.securemod.lock.LockManager.find(server.overworld(), chestPos[0]);
                return locked != null && locked.lock().checkPasscode("1234")
                        && com.takumistudios.securemod.data.ProtectedBlocksState.get(server).totalBlocks() >= 1;
            });
            if (!persisted) {
                throw new AssertionError("Las protecciones no persistieron tras reiniciar el mundo");
            }
        }
    }
}
