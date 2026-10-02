package com.takumistudios.securemod.config;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;

/** Interpreta las "raid windows" de la config ("SAT 18:00-22:00", "ALL 20:00-21:00"). Sin clases de Minecraft. */
public final class RaidWindowSchedule {
    private RaidWindowSchedule() {
    }

    public static boolean isActive(List<String> windows, LocalDateTime now) {
        for (String window : windows) {
            if (matches(window, now)) {
                return true;
            }
        }
        return false;
    }

    static boolean matches(String window, LocalDateTime now) {
        try {
            String[] parts = window.trim().split("\\s+");
            if (parts.length != 2) {
                return false;
            }
            String day = parts[0].toUpperCase(Locale.ROOT);
            if (!day.equals("ALL") && !now.getDayOfWeek().equals(parseDay(day))) {
                return false;
            }
            String[] range = parts[1].split("-");
            LocalTime start = LocalTime.parse(range[0]);
            LocalTime end = LocalTime.parse(range[1]);
            LocalTime time = now.toLocalTime();
            if (start.isBefore(end)) {
                return !time.isBefore(start) && time.isBefore(end);
            }
            // Ventana que cruza la medianoche
            return !time.isBefore(start) || time.isBefore(end);
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static DayOfWeek parseDay(String day) {
        for (DayOfWeek dow : DayOfWeek.values()) {
            if (dow.name().startsWith(day)) {
                return dow;
            }
        }
        throw new IllegalArgumentException(day);
    }
}
