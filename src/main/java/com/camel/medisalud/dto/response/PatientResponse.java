package com.camel.medisalud.dto.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PatientResponse(
        UUID id,
        String fullName,
        String document,
        String phone,
        String email,
        LocalDate birthDate,
        Instant createdAt,
        Instant updatedAt
) {
}
