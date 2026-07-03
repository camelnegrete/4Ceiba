package com.camel.medisalud.config;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "medisalud.scheduling")
public class SchedulingProperties {

    private ZoneId zone = ZoneId.of("America/Bogota");

    private LocalTime openingTime = LocalTime.of(8, 0);

    private LocalTime weekdayClosingTime = LocalTime.of(18, 0);

    private LocalTime saturdayClosingTime = LocalTime.of(13, 0);

    private int slotDurationMinutes = 30;

    private Set<DayOfWeek> workingDays = EnumSet.of(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY);

    private int lateCancellationHours = 2;

    private int maxPatientAgeYears = 120;

    private int penaltyBlockThreshold = 3;

    private int penaltyWindowDays = 30;
}
