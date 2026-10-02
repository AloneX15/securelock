package com.takumistudios.securemod.client.screen;

import com.takumistudios.securemod.network.Payloads;
import com.takumistudios.securemod.security.AccessMode;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Panel de configuración del bloque (se abre con el Codificador universal): propietario, modo,
 * jugadores permitidos/bloqueados, código y nivel de tarjeta. Cada acción se envía al servidor,
 * que valida el permiso y devuelve el estado actualizado.
 */
public class BlockConfigScreen extends Screen {
    private static final int PANEL_WIDTH = 260;
    private static final int MAX_LISTED = 5;

    private Payloads.OpenConfigS2C data;
    private EditBox codeBox;
    private EditBox nameBox;
    private String pendingCode = "";
    private String pendingName = "";

    public BlockConfigScreen(Payloads.OpenConfigS2C data) {
        super(Component.translatable("gui.securemod.config"));
        this.data = data;
    }

    public BlockPos pos() {
        return data.pos();
    }

    /** El servidor envió el estado actualizado. */
    public void update(Payloads.OpenConfigS2C newData) {
        this.data = newData;
        this.pendingCode = "";
        this.pendingName = nameBox == null ? "" : nameBox.getValue();
        this.rebuildWidgets();
    }

    @Override
    protected void init() {
        int left = (this.width - PANEL_WIDTH) / 2;
        int y = 46;

        AccessMode mode = AccessMode.byId(data.mode());
        addRenderableWidget(Button.builder(Component.translatable("gui.securemod.mode", Component.translatable("mode.securemod." + mode.id())),
                        b -> ClientPlayNetworking.send(new Payloads.SetModeC2S(data.pos(), mode.next().id(), data.cardLevel())))
                .bounds(left, y, PANEL_WIDTH / 2 - 2, 20).build());
        if (data.cardReader()) {
            int next = data.cardLevel() >= 5 ? 1 : Math.max(1, data.cardLevel()) + 1;
            addRenderableWidget(Button.builder(Component.translatable("gui.securemod.card_level", Math.max(1, data.cardLevel())),
                            b -> ClientPlayNetworking.send(new Payloads.SetModeC2S(data.pos(), mode.id(), next)))
                    .bounds(left + PANEL_WIDTH / 2 + 2, y, PANEL_WIDTH / 2 - 2, 20).build());
        }
        y += 26;

        if (data.usesCode()) {
            codeBox = new EditBox(this.font, left, y, 120, 20, Component.translatable("gui.securemod.new_code"));
            codeBox.setMaxLength(data.codeMax());
            codeBox.setHint(Component.translatable("gui.securemod.new_code").withStyle(ChatFormatting.DARK_GRAY));
            codeBox.setValue(pendingCode);
            codeBox.setResponder(value -> {
                String digits = value.replaceAll("[^0-9]", "");
                if (!digits.equals(value)) {
                    codeBox.setValue(digits);
                }
                pendingCode = digits;
            });
            addRenderableWidget(codeBox);
            addRenderableWidget(Button.builder(Component.translatable("gui.securemod.set_code"), b -> {
                String code = codeBox.getValue();
                if (code.length() >= data.codeMin()) {
                    ClientPlayNetworking.send(new Payloads.SetCodeC2S(data.pos(), code));
                }
            }).bounds(left + 124, y, 66, 20).build());
            Button remove = Button.builder(Component.translatable("gui.securemod.remove_code"),
                    b -> ClientPlayNetworking.send(new Payloads.SetCodeC2S(data.pos(), ""))).bounds(left + 194, y, 66, 20).build();
            remove.active = data.hasCode();
            addRenderableWidget(remove);
            y += 26;
        } else {
            codeBox = null;
        }

        nameBox = new EditBox(this.font, left, y, 120, 20, Component.translatable("gui.securemod.player_name"));
        nameBox.setMaxLength(16);
        nameBox.setHint(Component.translatable("gui.securemod.player_name").withStyle(ChatFormatting.DARK_GRAY));
        nameBox.setValue(pendingName);
        addRenderableWidget(nameBox);
        addRenderableWidget(Button.builder(Component.translatable("gui.securemod.allow"),
                b -> sendName(Payloads.ListAction.ALLOW_ADD)).bounds(left + 124, y, 66, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.securemod.deny"),
                b -> sendName(Payloads.ListAction.DENY_ADD)).bounds(left + 194, y, 66, 20).build());
        y += 34;

        addList(data.allowed(), left, y, Payloads.ListAction.ALLOW_REMOVE);
        addList(data.denied(), left + PANEL_WIDTH / 2 + 2, y, Payloads.ListAction.DENY_REMOVE);

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(this.width / 2 - 50, this.height - 28, 100, 20).build());
    }

    private void addList(List<String> names, int x, int y, Payloads.ListAction removeAction) {
        int shown = Math.min(names.size(), MAX_LISTED);
        for (int i = 0; i < shown; i++) {
            String name = names.get(i);
            addRenderableWidget(Button.builder(Component.literal("✕"), b -> ClientPlayNetworking.send(new Payloads.UpdateAllowListC2S(data.pos(), removeAction, name)))
                    .bounds(x + PANEL_WIDTH / 2 - 22, y + 12 + i * 14, 14, 12).build());
        }
    }

    private void sendName(Payloads.ListAction action) {
        String name = nameBox.getValue().trim();
        if (name.matches("[A-Za-z0-9_]{1,16}")) {
            ClientPlayNetworking.send(new Payloads.UpdateAllowListC2S(data.pos(), action, name));
            nameBox.setValue("");
            pendingName = "";
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        int left = (this.width - PANEL_WIDTH) / 2;
        graphics.centeredText(this.font, Component.translatable(data.blockKey()), this.width / 2, 12, 0xFFFFFFFF);
        graphics.centeredText(this.font, Component.translatable("gui.securemod.owner", data.ownerName())
                .append(data.hasCode() ? Component.literal("  ·  ").append(Component.translatable("hud.securemod.passcode")) : Component.empty()),
                this.width / 2, 26, 0xFFFFD27F);

        int listY = 46 + 26 + (data.usesCode() ? 26 : 0) + 34;
        drawList(graphics, Component.translatable("gui.securemod.allowed"), data.allowed(), left, listY, 0xFF55FF55);
        drawList(graphics, Component.translatable("gui.securemod.denied"), data.denied(), left + PANEL_WIDTH / 2 + 2, listY, 0xFFFF5555);
    }

    private void drawList(GuiGraphicsExtractor graphics, Component title, List<String> names, int x, int y, int color) {
        graphics.text(this.font, title, x, y, color);
        int shown = Math.min(names.size(), MAX_LISTED);
        for (int i = 0; i < shown; i++) {
            graphics.text(this.font, names.get(i), x + 4, y + 14 + i * 14, 0xFFE0E0E0);
        }
        if (names.isEmpty()) {
            graphics.text(this.font, Component.literal("-"), x + 4, y + 14, 0xFF808080);
        } else if (names.size() > MAX_LISTED) {
            graphics.text(this.font, Component.literal("+" + (names.size() - MAX_LISTED)), x + 4, y + 14 + MAX_LISTED * 14, 0xFF808080);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
