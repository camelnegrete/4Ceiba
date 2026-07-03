package com.camel.medisalud.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DoctorRequest(

        @Schema(example = "Dr. Gregory House")
        @NotBlank(message = "fullName is required")
        @Size(min = 3, max = 100, message = "fullName must be between 3 and 100 characters")
        String fullName,

        @Schema(example = "Diagnostics")
        @NotBlank(message = "specialty is required")
        @Size(max = 100, message = "specialty must not exceed 100 characters")
        String specialty,

        @Schema(example = "3001234567")
        @Size(min = 7, max = 30, message = "phone must have at least 7 characters")
        String phone,

        @Schema(example = "house@clinic.com")
        @Email(message = "email must be valid")
        @Size(max = 150, message = "email must not exceed 150 characters")
        String email
) {
}
