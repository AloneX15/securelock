package com.takumistudios.securemod.config;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RaidWindowScheduleTest {
    // 2026-10-03 es sábado
    private static final LocalDateTime SAT_19 = LocalDateTime.of(2026, 10, 3, 19, 0);
    private static final LocalDateTime SAT_23 = LocalDateTime.of(2026, 10, 3, 23, 0);
    private static final LocalDateTime SUN_01 = LocalDateTime.of(2026, 10, 4, 1, 0);

    @Test
    void matchesDayAndTime() {
        List<String> windows = List.of("SAT 18:00-22:00");
        assertTrue(RaidWindowSchedule.isActive(windows, SAT_19));
        assertFalse(RaidWindowSchedule.isActive(windows, SAT_23));
        assertFalse(RaidWindowSchedule.isActive(windows, SUN_01));
    }

    @Test
    void allDaysAndMidnightCrossing() {
        assertTrue(RaidWindowSchedule.isActive(List.of("ALL 22:30-02:00"), SAT_23));
        assertTrue(RaidWindowSchedule.isActive(List.of("ALL 22:30-02:00"), SUN_01));
        assertFalse(RaidWindowSchedule.isActive(List.of("ALL 22:30-02:00"), SAT_19));
    }

    @Test
    void malformedWindowsAreIgnored() {
        assertFalse(RaidWindowSchedule.isActive(List.of("", "SAT", "XYZ 10:00-11:00", "SAT 25:00-26:00", "SAT 18-22"), SAT_19));
    }
}
