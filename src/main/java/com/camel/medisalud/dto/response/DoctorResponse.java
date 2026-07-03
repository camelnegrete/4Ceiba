package com.camel.medisalud.dto.response;

import java.time.Instant;
import java.util.UUID;

public record DoctorResponse(
        UUID id,
        String fullName,
        String specialty,
        String phone,
        String email,
        Instant createdAt,
        Instant updatedAt
) {
}
