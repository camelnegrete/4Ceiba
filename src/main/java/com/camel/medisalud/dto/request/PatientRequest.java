package com.camel.medisalud.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record PatientRequest(

        @Schema(example = "John Doe")
        @NotBlank(message = "fullName is required")
        @Size(min = 3, max = 100, message = "fullName must be between 3 and 100 characters")
        String fullName,

        @Schema(example = "1010101010")
        @NotBlank(message = "document is required")
        @Size(max = 50, message = "document must not exceed 50 characters")
        String document,

        @Schema(example = "3001234567")
        @NotBlank(message = "phone is required")
        @Size(max = 30, message = "phone must not exceed 30 characters")
        String phone,

        @Schema(example = "john.doe@mail.com")
        @NotBlank(message = "email is required")
        @Email(message = "email must be valid")
        @Size(max = 150, message = "email must not exceed 150 characters")
        String email,

        @Schema(example = "1990-05-20")
        @Past(message = "birthDate must be in the past")
        LocalDate birthDate
) {
}
