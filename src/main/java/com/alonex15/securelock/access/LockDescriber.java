package com.alonex15.securelock.access;

import com.alonex15.securelock.lock.LockData;
import com.alonex15.securelock.lock.LockManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.function.Consumer;

/** Texto con la información de un bloque protegido (para /securelock info y la Admin Tool). */
public final class LockDescriber {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    private LockDescriber() {
    }

    public static void describe(LockManager.Locked locked, boolean admin, Consumer<Component> out) {
        LockData lock = locked.lock();
        out.accept(Component.translatable("info.securelock.header", locked.pos().getX(), locked.pos().getY(), locked.pos().getZ())
                .withStyle(ChatFormatting.GOLD));
        out.accept(line("info.securelock.owner", Component.literal(lock.ownerName())));
        out.accept(line("info.securelock.mode", Component.translatable("mode.securelock." + lock.mode().id())));
        out.accept(line("info.securelock.kind", Component.translatable("kind.securelock." + lock.kind().id())));
        if (!lock.allowed().isEmpty()) {
            out.accept(line("info.securelock.allowed", Component.literal(join(lock.allowed().values()))));
        }
        if (!lock.denied().isEmpty()) {
            out.accept(line("info.securelock.denied", Component.literal(join(lock.denied().values()))));
        }
        out.accept(line("info.securelock.passcode", Component.translatable(lock.hasPasscode() ? "gui.yes" : "gui.no")));
        if (lock.cardLevel() > 0) {
            out.accept(line("info.securelock.card_level", Component.literal(String.valueOf(lock.cardLevel()))));
        }
        if (admin) {
            out.accept(line("info.securelock.block", Component.literal(lock.blockId())));
            out.accept(line("info.securelock.created", Component.literal(lock.createdAt() > 0 ? DATE.format(Instant.ofEpochMilli(lock.createdAt())) : "?")));
            out.accept(line("info.securelock.owner_uuid", Component.literal(lock.owner().toString())));
            if (lock.isCorrupt()) {
                out.accept(Component.translatable("info.securelock.corrupt").withStyle(ChatFormatting.RED));
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
