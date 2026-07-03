package com.camel.medisalud.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record RescheduleAppointmentRequest(

        @NotNull
        Instant newAppointmentDateTime
) {
}
