package com.alonex15.securelock.client;

import com.alonex15.securelock.SecureLock;
import com.alonex15.securelock.client.compat.ClientCompat;
import com.alonex15.securelock.client.screen.BlockConfigScreen;
import com.alonex15.securelock.client.screen.KeypadScreen;
import com.alonex15.securelock.network.Payloads;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public final class SecureLockClient implements ClientModInitializer {
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
                Identifier.fromNamespaceAndPath(SecureLock.MOD_ID, "owner_hud"), OwnerHud::extractRenderState);
    }
}
