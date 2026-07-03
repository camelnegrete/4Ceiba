package com.camel.medisalud.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record AppointmentRequest(

        @NotNull
        UUID doctorId,

        @NotNull
        UUID patientId,

        @NotNull
        Instant appointmentDateTime
) {
}
