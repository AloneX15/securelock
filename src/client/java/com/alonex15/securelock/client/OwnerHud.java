package com.alonex15.securelock.client;

import com.alonex15.securelock.network.Payloads;
import com.alonex15.securelock.client.compat.ClientCompat;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jspecify.annotations.Nullable;

/** HUD opcional: al mirar un bloque protegido muestra "Propiedad de X" bajo la mira. */
public final class OwnerHud {
    private static final boolean JADE = FabricLoader.getInstance().isModLoaded("jade");
    private static Payloads.@Nullable OwnerInfoS2C info;

    private OwnerHud() {
    }

    public static void update(Payloads.OwnerInfoS2C payload) {
        info = payload.isProtected() ? payload : null;
    }

    public static void clear() {
        info = null;
    }

    public static Payloads.@Nullable OwnerInfoS2C current() {
        return info;
    }

    public static void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Payloads.OwnerInfoS2C current = info;
        Minecraft minecraft = Minecraft.getInstance();
        // Con Jade instalado, la información aparece en su tooltip y no se duplica
        if (current == null || JADE || ClientCompat.currentScreen() != null) {
            return;
        }
        HitResult hit = minecraft.hitResult;
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK || !blockHit.getBlockPos().equals(current.pos())) {
            return;
        }
        Component line = Component.translatable("hud.securelock.owned_by", current.ownerName());
        Component detail = Component.translatable("mode.securelock." + current.mode())
                .append(current.hasCode() ? Component.literal(" · ").append(Component.translatable("hud.securelock.passcode")) : Component.empty());
        int x = graphics.guiWidth() / 2;
        int y = graphics.guiHeight() / 2 + 12;
        graphics.centeredText(minecraft.font, line, x, y, 0xFFFFD27F);
        graphics.centeredText(minecraft.font, detail, x, y + 10, 0xFFB0B0B0);
    }
}
