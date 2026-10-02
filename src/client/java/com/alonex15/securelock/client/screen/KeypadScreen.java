package com.alonex15.securelock.client.screen;

import com.alonex15.securelock.network.Payloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import com.mojang.blaze3d.platform.InputConstants;

/** Teclado numérico: botones 0–9, borrar y OK. También acepta el teclado del ordenador. */
public class KeypadScreen extends Screen {
    private static final int BUTTON = 22;
    private static final int GAP = 4;

    private final BlockPos pos;
    private final int minLength;
    private final int maxLength;
    private final StringBuilder code = new StringBuilder();
    private Component status = Component.empty();
    private int statusColor = 0xFFFFFFFF;
    private boolean waiting;

    public KeypadScreen(BlockPos pos, int minLength, int maxLength) {
        super(Component.translatable("gui.securelock.keypad"));
        this.pos = pos;
        this.minLength = Math.max(1, minLength);
        this.maxLength = Math.max(this.minLength, Math.min(16, maxLength));
    }

    @Override
    protected void init() {
        int gridWidth = 3 * BUTTON + 2 * GAP;
        int left = (this.width - gridWidth) / 2;
        int top = this.height / 2 - 40;
        String[] labels = {"1", "2", "3", "4", "5", "6", "7", "8", "9"};
        for (int i = 0; i < labels.length; i++) {
            String digit = labels[i];
            int x = left + (i % 3) * (BUTTON + GAP);
            int y = top + (i / 3) * (BUTTON + GAP);
            addRenderableWidget(Button.builder(Component.literal(digit), b -> type(digit)).bounds(x, y, BUTTON, BUTTON).build());
        }
        int lastRow = top + 3 * (BUTTON + GAP);
        addRenderableWidget(Button.builder(Component.literal("C").withStyle(ChatFormatting.RED), b -> clear())
                .bounds(left, lastRow, BUTTON, BUTTON).build());
        addRenderableWidget(Button.builder(Component.literal("0"), b -> type("0"))
                .bounds(left + BUTTON + GAP, lastRow, BUTTON, BUTTON).build());
        addRenderableWidget(Button.builder(Component.literal("OK").withStyle(ChatFormatting.GREEN), b -> submit())
                .bounds(left + 2 * (BUTTON + GAP), lastRow, BUTTON, BUTTON).build());
    }

    private void type(String digit) {
        if (code.length() < maxLength && !waiting) {
            code.append(digit);
        }
    }

    private void clear() {
        code.setLength(0);
    }

    private void submit() {
        if (code.length() < minLength || waiting) {
            return;
        }
        waiting = true;
        ClientPlayNetworking.send(new Payloads.SubmitCodeC2S(pos, code.toString()));
        code.setLength(0);
    }

    /** Respuesta del servidor. */
    public void onResult(Payloads.CodeResultS2C result) {
        waiting = false;
        switch (result.status()) {
            case OK -> onClose();
            case WRONG -> {
                status = Component.translatable("gui.securelock.keypad.wrong", result.attemptsLeft());
                statusColor = 0xFFFF5555;
            }
            case LOCKED_OUT -> {
                status = Component.translatable("gui.securelock.keypad.locked", result.secondsRemaining());
                statusColor = 0xFFFFAA00;
            }
            case INVALID -> {
                status = Component.translatable("gui.securelock.keypad.invalid");
                statusColor = 0xFFFF5555;
            }
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == InputConstants.KEY_BACKSPACE && !code.isEmpty()) {
            code.setLength(code.length() - 1);
            return true;
        }
        if (event.key() == InputConstants.KEY_RETURN || event.key() == InputConstants.KEY_NUMPADENTER) {
            submit();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        int c = event.codepoint();
        if (c >= '0' && c <= '9') {
            type(Character.toString(c));
            return true;
        }
        return super.charTyped(event);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        int center = this.width / 2;
        int top = this.height / 2 - 40;
        graphics.centeredText(this.font, this.title, center, top - 36, 0xFFFFFFFF);
        int boxWidth = 3 * BUTTON + 2 * GAP;
        graphics.fill(center - boxWidth / 2, top - 22, center + boxWidth / 2, top - 6, 0xFF101010);
        graphics.outline(center - boxWidth / 2, top - 22, boxWidth, 16, 0xFF5A5A5A);
        String masked = "*".repeat(code.length());
        graphics.centeredText(this.font, masked.isEmpty() ? "-" : masked, center, top - 18, 0xFF55FF55);
        graphics.centeredText(this.font, status, center, top + 4 * (BUTTON + GAP) + 4, statusColor);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
