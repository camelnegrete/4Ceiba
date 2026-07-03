package com.camel.medisalud.service.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.camel.medisalud.config.SchedulingProperties;
import com.camel.medisalud.domain.exception.ValidationException;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import org.junit.jupiter.api.Test;

class WorkingHoursPolicyTest {

    private final SchedulingProperties properties = new SchedulingProperties();
    private final WorkingHoursPolicy policy = new WorkingHoursPolicy(properties);
    private final ZoneId zone = properties.getZone();

    private final LocalDate monday =
            LocalDate.now(zone).with(TemporalAdjusters.next(DayOfWeek.MONDAY));
    private final LocalDate saturday =
            LocalDate.now(zone).with(TemporalAdjusters.next(DayOfWeek.SATURDAY));
    private final LocalDate sunday =
            LocalDate.now(zone).with(TemporalAdjusters.next(DayOfWeek.SUNDAY));

    private Instant at(LocalDate date, int hour, int minute) {
        return date.atTime(hour, minute).atZone(zone).toInstant();
    }

    @Test
    void validSlot_passes() {
        assertThatCode(() -> policy.validateSlot(at(monday, 9, 0)))
                .doesNotThrowAnyException();
    }

    @Test
    void beforeOpening_isRejected() {
        assertThatThrownBy(() -> policy.validateSlot(at(monday, 7, 0)))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void lastSlotThatOverflowsClosing_isRejected() {

        assertThatThrownBy(() -> policy.validateSlot(at(monday, 18, 0)))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void sunday_isClosedAndRejected() {
        assertThatThrownBy(() -> policy.validateSlot(at(sunday, 9, 0)))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void saturdayMorning_isAccepted() {
        assertThatCode(() -> policy.validateSlot(at(saturday, 12, 30)))
                .doesNotThrowAnyException();
    }

    @Test
    void saturdayAfternoon_isRejected() {

        assertThatThrownBy(() -> policy.validateSlot(at(saturday, 13, 0)))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void misalignedTime_isRejected() {
        assertThatThrownBy(() -> policy.validateSlot(at(monday, 9, 15)))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void slotsFor_weekday_returns20AlignedSlots() {

        assertThat(policy.slotsFor(monday))
                .hasSize(20)
                .startsWith(at(monday, 8, 0))
                .endsWith(at(monday, 17, 30))
                .doesNotContain(at(monday, 18, 0));
    }

    @Test
    void slotsFor_saturday_returns10AlignedSlots() {

        assertThat(policy.slotsFor(saturday))
                .hasSize(10)
                .startsWith(at(saturday, 8, 0))
                .endsWith(at(saturday, 12, 30))
                .doesNotContain(at(saturday, 13, 0));
    }

    @Test
    void slotsFor_sunday_isEmpty() {
        assertThat(policy.slotsFor(sunday)).isEmpty();
    }

    @Test
    void isLateCancellation_withinWindow_isTrue() {
        Instant appointment = at(monday, 9, 0);
        assertThat(policy.isLateCancellation(appointment, appointment.minus(Duration.ofHours(1))))
                .isTrue();
    }

    @Test
    void isLateCancellation_beforeWindow_isFalse() {
        Instant appointment = at(monday, 9, 0);
        assertThat(policy.isLateCancellation(appointment, appointment.minus(Duration.ofDays(3))))
                .isFalse();
    }
}
