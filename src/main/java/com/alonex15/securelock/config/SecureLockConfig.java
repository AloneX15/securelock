package com.alonex15.securelock.config;

import com.alonex15.securelock.SecureLock;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Configuración en {@code config/securelock.json}. Sin dependencias externas: Gson ya viene con Minecraft.
 * Si el archivo está corrupto se usan los valores por defecto y se avisa en el log (nunca crashea).
 */
public final class SecureLockConfig {
    public static final int CURRENT_VERSION = 1;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static volatile SecureLockConfig instance = new SecureLockConfig();

    public int configVersion = CURRENT_VERSION;

    /** Perfil preconfigurado: custom, friends, survival o factions. Distinto de custom sobrescribe algunos valores. */
    public String profile = "custom";

    // --- Contraseñas y fuerza bruta ---
    public int maxFailedAttempts = 3;
    public int lockoutSeconds = 30;
    public int codeMinLength = 4;
    public int codeMaxLength = 16;
    public int sessionSeconds = 10;

    // --- Protecciones ---
    public boolean allowHoppersFromSameOwner = true;
    public boolean comparatorOutputHidden = true;
    public boolean explosionProof = true;
    public boolean allowTeamAccess = true;
    public boolean protectSupportBlocks = true;
    /** Integración con claims (OPAC, Flan): quien tenga acceso al claim también puede usar los bloques protegidos. */
    public boolean claimsGrantAccess = false;

    // --- Límites y purga ---
    public int maxProtectedBlocksPerPlayer = 0;
    public int inactiveOwnerDays = 0;
    public boolean autoPurgeInactiveOwners = false;

    // --- Red y servidor ---
    public double maxInteractDistance = 8.0;
    public int maxPacketsPerSecond = 10;
    public boolean ownerHud = true;

    // --- Redstone ---
    public int keypadSignalTicks = 60;
    public int cardReaderSignalTicks = 60;
    public int scannerSignalTicks = 40;

    // --- Registro de auditoría ---
    public boolean auditLog = true;

    // --- Jugadores falsos de otros mods (FakePlayer) ---
    public boolean fakePlayersUseOwnerPermissions = true;
    public List<String> allowedFakePlayerNames = new ArrayList<>();

    // --- Padlock ---
    /** Bloques extra (ids) que el Padlock puede bloquear además del tag securelock:lockable. */
    public List<String> extraLockableBlocks = new ArrayList<>();
    /** Bloques (ids) que nunca se pueden bloquear, además del tag securelock:never_lock. */
    public List<String> neverLockableBlocks = new ArrayList<>();

    // --- Raids (servidores de facciones) ---
    /** Si es true, durante una "raid window" los bloques protegidos se pueden romper y explotar. */
    public boolean raidWindowsEnabled = false;
    /** Ventanas con formato "SAT 18:00-22:00" (hora del servidor). Días: MON..SUN o ALL. */
    public List<String> raidWindows = new ArrayList<>();

    // --- Compatibilidad ---
    public Compat compat = new Compat();

    public static final class Compat {
        /**
         * Funciones desactivadas: hopper_protection, explosion_protection, piston_protection,
         * fire_protection, comparator_protection, transfer_protection, mob_protection, golem_protection.
         */
        public List<String> disable = new ArrayList<>();
    }

    public static SecureLockConfig get() {
        return instance;
    }

    public boolean isFeatureDisabled(String feature) {
        return compat != null && compat.disable != null && compat.disable.contains(feature.toLowerCase(Locale.ROOT));
    }

    public static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve(SecureLock.MOD_ID + ".json");
    }

    /** Carga (o crea) el archivo. Devuelve un mensaje de error o {@code null} si todo fue bien. */
    public static String load() {
        Path path = path();
        SecureLockConfig loaded = new SecureLockConfig();
        String error = null;
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                SecureLockConfig parsed = GSON.fromJson(reader, SecureLockConfig.class);
                if (parsed != null) {
                    loaded = parsed;
                }
            } catch (IOException | JsonParseException e) {
                error = "No se pudo leer " + path + ": " + e.getMessage() + ". Se usan los valores por defecto.";
                SecureLock.LOGGER.warn(error);
            }
        }
        loaded.sanitize();
        loaded.applyProfile();
        instance = loaded;
        if (error == null) {
            save(loaded, path);
        }
        return error;
    }

    private static void save(SecureLockConfig config, Path path) {
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(config, writer);
            }
        } catch (IOException e) {
            SecureLock.LOGGER.warn("No se pudo guardar {}: {}", path, e.getMessage());
        }
    }

    private void sanitize() {
        maxFailedAttempts = clamp(maxFailedAttempts, 1, 100);
        lockoutSeconds = clamp(lockoutSeconds, 0, 86_400);
        codeMinLength = clamp(codeMinLength, 1, 16);
        codeMaxLength = clamp(codeMaxLength, codeMinLength, 16);
        sessionSeconds = clamp(sessionSeconds, 0, 3600);
        maxProtectedBlocksPerPlayer = Math.max(0, maxProtectedBlocksPerPlayer);
        inactiveOwnerDays = Math.max(0, inactiveOwnerDays);
        maxInteractDistance = Math.clamp(maxInteractDistance, 1.0, 64.0);
        maxPacketsPerSecond = clamp(maxPacketsPerSecond, 1, 200);
        keypadSignalTicks = clamp(keypadSignalTicks, 2, 1200);
        cardReaderSignalTicks = clamp(cardReaderSignalTicks, 2, 1200);
        scannerSignalTicks = clamp(scannerSignalTicks, 2, 1200);
        if (profile == null) profile = "custom";
        if (allowedFakePlayerNames == null) allowedFakePlayerNames = new ArrayList<>();
        if (extraLockableBlocks == null) extraLockableBlocks = new ArrayList<>();
        if (neverLockableBlocks == null) neverLockableBlocks = new ArrayList<>();
        if (raidWindows == null) raidWindows = new ArrayList<>();
        if (compat == null) compat = new Compat();
        if (compat.disable == null) compat.disable = new ArrayList<>();
        compat.disable.replaceAll(s -> s.toLowerCase(Locale.ROOT));
    }

    /** Perfiles preconfigurados: desde un survival entre amigos hasta un servidor de facciones. */
    private void applyProfile() {
        switch (profile.toLowerCase(Locale.ROOT)) {
            case "friends" -> {
                allowTeamAccess = true;
                maxFailedAttempts = 5;
                lockoutSeconds = 10;
                maxProtectedBlocksPerPlayer = 0;
                raidWindowsEnabled = false;
            }
            case "survival" -> {
                allowTeamAccess = true;
                maxFailedAttempts = 3;
                lockoutSeconds = 30;
                raidWindowsEnabled = false;
            }
            case "factions" -> {
                allowTeamAccess = true;
                maxFailedAttempts = 3;
                lockoutSeconds = 120;
                raidWindowsEnabled = true;
            }
            default -> {
            }
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
