package com.takumistudios.securemod.access;

import com.takumistudios.securemod.lock.LockData;
import com.takumistudios.securemod.lock.LockManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.function.Consumer;

/** Texto con la información de un bloque protegido (para /securemod info y la Admin Tool). */
public final class LockDescriber {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    private LockDescriber() {
    }

    public static void describe(LockManager.Locked locked, boolean admin, Consumer<Component> out) {
        LockData lock = locked.lock();
        out.accept(Component.translatable("info.securemod.header", locked.pos().getX(), locked.pos().getY(), locked.pos().getZ())
                .withStyle(ChatFormatting.GOLD));
        out.accept(line("info.securemod.owner", Component.literal(lock.ownerName())));
        out.accept(line("info.securemod.mode", Component.translatable("mode.securemod." + lock.mode().id())));
        out.accept(line("info.securemod.kind", Component.translatable("kind.securemod." + lock.kind().id())));
        if (!lock.allowed().isEmpty()) {
            out.accept(line("info.securemod.allowed", Component.literal(join(lock.allowed().values()))));
        }
        if (!lock.denied().isEmpty()) {
            out.accept(line("info.securemod.denied", Component.literal(join(lock.denied().values()))));
        }
        out.accept(line("info.securemod.passcode", Component.translatable(lock.hasPasscode() ? "gui.yes" : "gui.no")));
        if (lock.cardLevel() > 0) {
            out.accept(line("info.securemod.card_level", Component.literal(String.valueOf(lock.cardLevel()))));
        }
        if (admin) {
            out.accept(line("info.securemod.block", Component.literal(lock.blockId())));
            out.accept(line("info.securemod.created", Component.literal(lock.createdAt() > 0 ? DATE.format(Instant.ofEpochMilli(lock.createdAt())) : "?")));
            out.accept(line("info.securemod.owner_uuid", Component.literal(lock.owner().toString())));
            if (lock.isCorrupt()) {
                out.accept(Component.translatable("info.securemod.corrupt").withStyle(ChatFormatting.RED));
            }
        }
    }

    private static MutableComponent line(String key, Component value) {
        return Component.translatable(key).withStyle(ChatFormatting.GRAY).append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                .append(value.copy().withStyle(ChatFormatting.WHITE));
    }

    private static String join(Collection<String> names) {
        return String.join(", ", names);
    }
}
