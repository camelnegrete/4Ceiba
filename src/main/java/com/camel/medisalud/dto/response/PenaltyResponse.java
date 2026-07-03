package com.camel.medisalud.dto.response;

import com.camel.medisalud.domain.enums.PenaltyReason;
import java.time.Instant;
import java.util.UUID;

public record PenaltyResponse(
        UUID id,
        PatientResponse patient,
        AppointmentResponse appointment,
        PenaltyReason reason,
        Instant createdAt,
        Instant updatedAt
) {
}
