package com.guiltfree.tracker.service;

import com.guiltfree.tracker.model.PayFrequency;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;

// Translates a pay frequency into real paycheck counts.
@Service
public class PayScheduleService {

    // How many paychecks a year for this frequency.
    public int paychecksPerYear(PayFrequency frequency) {
        return switch (frequency) {
            case MONTHLY -> 12;
            case SEMI_MONTHLY -> 24;
            case BIWEEKLY -> 26;
            case WEEKLY -> 52;
        };
    }

    // How many paychecks land within the given month, for this specific calendar month.
    public int paychecksInMonth(PayFrequency frequency, LocalDate anchorPayDate, YearMonth month) {
        return switch (frequency) {
            case MONTHLY -> 1;
            case SEMI_MONTHLY -> 2;
            case BIWEEKLY -> countPaydaysInMonth(anchorPayDate, month, 14);
            case WEEKLY -> countPaydaysInMonth(anchorPayDate, month, 7);
        };
    }

    // Walks every day in the month and counts days that land exactly on a pay cycle.
    private int countPaydaysInMonth(LocalDate anchorPayDate, YearMonth month, int periodDays) {
        int count = 0;
        LocalDate day = month.atDay(1);
        LocalDate lastDay = month.atEndOfMonth();
        while (!day.isAfter(lastDay)) {
            long daysSinceAnchor = ChronoUnit.DAYS.between(anchorPayDate, day);
            if (Math.floorMod(daysSinceAnchor, periodDays) == 0) {
                count++;
            }
            day = day.plusDays(1);
        }
        return count;
    }
}
