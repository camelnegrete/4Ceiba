package com.camel.medisalud.service.scheduling;

import com.camel.medisalud.config.SchedulingProperties;
import com.camel.medisalud.domain.exception.ValidationException;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WorkingHoursPolicy {

    private final SchedulingProperties properties;

    public void validateSlot(Instant when) {
        ZonedDateTime zoned = when.atZone(properties.getZone());
        LocalTime closing = closingTimeFor(zoned.getDayOfWeek())
                .orElseThrow(() -> new ValidationException(
                        "The clinic does not provide attention on " + zoned.getDayOfWeek()));

        LocalTime time = zoned.toLocalTime();
        if (time.getSecond() != 0 || time.getNano() != 0 || !isSlotAligned(time)) {
            throw new ValidationException(
                    "Appointment time must align to a " + properties.getSlotDurationMinutes()
                            + "-minute slot starting at " + properties.getOpeningTime());
        }
        if (time.isBefore(properties.getOpeningTime()) || !fitsBeforeClosing(time, closing)) {
            throw new ValidationException("Appointment time is outside working hours ("
                    + properties.getOpeningTime() + " - " + closing + ")");
        }
    }

    public List<Instant> slotsFor(LocalDate date) {
        Optional<LocalTime> closing = closingTimeFor(date.getDayOfWeek());
        if (closing.isEmpty()) {
            return List.of();
        }
        List<Instant> slots = new ArrayList<>();
        for (LocalTime time = properties.getOpeningTime();
             fitsBeforeClosing(time, closing.get());
             time = time.plusMinutes(properties.getSlotDurationMinutes())) {
            slots.add(date.atTime(time).atZone(properties.getZone()).toInstant());
        }
        return slots;
    }

    public Optional<LocalTime> closingTimeFor(DayOfWeek day) {
        if (!properties.getWorkingDays().contains(day)) {
            return Optional.empty();
        }
        return Optional.of(day == DayOfWeek.SATURDAY
                ? properties.getSaturdayClosingTime()
                : properties.getWeekdayClosingTime());
    }

    public boolean isLateCancellation(Instant appointmentDateTime, Instant now) {
        Instant threshold = appointmentDateTime.minus(
                Duration.ofHours(properties.getLateCancellationHours()));
        return !now.isBefore(threshold);
    }

    public Instant startOfDay(LocalDate date) {
        return date.atStartOfDay(properties.getZone()).toInstant();
    }

    public Instant endOfDay(LocalDate date) {
        return date.plusDays(1).atStartOfDay(properties.getZone()).toInstant();
    }

    private boolean fitsBeforeClosing(LocalTime slotStart, LocalTime closing) {
        return !slotStart.plusMinutes(properties.getSlotDurationMinutes()).isAfter(closing);
    }

    private boolean isSlotAligned(LocalTime time) {
        long minutesFromOpening = Duration.between(properties.getOpeningTime(), time).toMinutes();
        return minutesFromOpening >= 0
                && minutesFromOpening % properties.getSlotDurationMinutes() == 0;
    }
}
