package com.takumistudios.securemod.client;

import com.takumistudios.securemod.SecureMod;
import com.takumistudios.securemod.client.compat.ClientCompat;
import com.takumistudios.securemod.client.screen.BlockConfigScreen;
import com.takumistudios.securemod.client.screen.KeypadScreen;
import com.takumistudios.securemod.network.Payloads;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public final class SecureModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(Payloads.OpenKeypadS2C.TYPE, (payload, context) ->
                ClientCompat.setScreen(new KeypadScreen(payload.pos(), payload.minLength(), payload.maxLength())));
        ClientPlayNetworking.registerGlobalReceiver(Payloads.CodeResultS2C.TYPE, (payload, context) -> {
            if (ClientCompat.currentScreen() instanceof KeypadScreen keypad) {
                keypad.onResult(payload);
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(Payloads.OpenConfigS2C.TYPE, (payload, context) -> {
            if (ClientCompat.currentScreen() instanceof BlockConfigScreen current && current.pos().equals(payload.pos())) {
                current.update(payload);
            } else {
                ClientCompat.setScreen(new BlockConfigScreen(payload));
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(Payloads.OwnerInfoS2C.TYPE, (payload, context) -> OwnerHud.update(payload));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> OwnerHud.clear());

        HudElementRegistry.attachElementAfter(VanillaHudElements.CROSSHAIR,
                Identifier.fromNamespaceAndPath(SecureMod.MOD_ID, "owner_hud"), OwnerHud::extractRenderState);
    }
}
