package com.alonex15.securelock.security;

import com.alonex15.securelock.SecureLock;
import com.alonex15.securelock.config.SecureLockConfig;
import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.stream.Stream;

/**
 * Registro de auditoría en {@code logs/securelock-audit.log}: colocación, rotura, acceso denegado,
 * cambio de código, bypass de admin... Formato: una línea por evento, campos separados por " | ".
 */
public final class AuditLogger {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static BufferedWriter writer;
    private static boolean failed;

    private AuditLogger() {
    }

    public enum Event {
        PLACE, LOCK, UNLOCK, BREAK, ACCESS_DENIED, CODE_OK, CODE_FAIL, CODE_LOCKOUT, CODE_CHANGED,
        ALLOW_ADD, ALLOW_REMOVE, DENY_ADD, DENY_REMOVE, MODE_CHANGED, CARD_LEVEL_CHANGED, CARD_LINKED,
        ADMIN_BYPASS, ADMIN_UNLOCK, ADMIN_TRANSFER, ADMIN_PURGE, ADMIN_UNLOCK_ALL, ADMIN_ROLLBACK, TRUST, UNTRUST,
        PACKET_REJECTED, RATE_LIMITED
    }

    public static Path path() {
        return FabricLoader.getInstance().getGameDir().resolve("logs").resolve("securelock-audit.log");
    }

    /** Escribe un evento. Nunca lanza excepciones: si falla, avisa una sola vez y desactiva el registro. */
    public static synchronized void log(Event event, String actor, String location, String details) {
        if (!SecureLockConfig.get().auditLog || failed) {
            return;
        }
        try {
            if (writer == null) {
                Path path = path();
                Files.createDirectories(path.getParent());
                writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            }
            writer.write(String.join(" | ", LocalDateTime.now().format(TIME), event.name(), sanitize(actor), sanitize(location), sanitize(details)));
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            failed = true;
            SecureLock.LOGGER.warn("No se pudo escribir el registro de auditoría; se desactiva: {}", e.getMessage());
        }
    }

    public static synchronized void close() {
        if (writer != null) {
            try {
                writer.close();
            } catch (IOException ignored) {
            }
            writer = null;
        }
        failed = false;
    }

    /** Últimas {@code limit} líneas que contienen {@code needle} (para {@code /securelock inspect}). */
    public static synchronized List<String> search(String needle, int limit) {
        Path path = path();
        if (!Files.exists(path)) {
            return List.of();
        }
        Deque<String> result = new ArrayDeque<>(limit);
        try (Stream<String> lines = Files.lines(path, StandardCharsets.UTF_8)) {
            lines.filter(line -> line.contains(needle)).forEach(line -> {
                if (result.size() == limit) {
                    result.removeFirst();
                }
                result.addLast(line);
            });
        } catch (IOException | java.io.UncheckedIOException e) {
            return List.of();
        }
        return new ArrayList<>(result);
    }

    private static String sanitize(String value) {
        return value == null ? "-" : value.replace('\n', ' ').replace('\r', ' ').replace("|", "/");
    }
}
