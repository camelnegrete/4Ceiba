package com.camel.medisalud.dto.response;

import com.camel.medisalud.domain.enums.AppointmentStatus;
import java.time.Instant;
import java.util.UUID;

public record AppointmentResponse(
        UUID id,
        DoctorResponse doctor,
        PatientResponse patient,
        Instant appointmentDateTime,
        AppointmentStatus status,
        Instant cancelledAt,
        Instant createdAt,
        Instant updatedAt
) {
}
