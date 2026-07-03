package com.camel.medisalud.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.camel.medisalud.config.SchedulingProperties;
import com.camel.medisalud.domain.exception.ConflictException;
import com.camel.medisalud.domain.exception.ValidationException;
import com.camel.medisalud.domain.model.Patient;
import com.camel.medisalud.dto.request.PatientRequest;
import com.camel.medisalud.dto.response.PatientResponse;
import com.camel.medisalud.mapper.PatientMapper;
import com.camel.medisalud.repository.PatientRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PatientServiceImplTest {

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private PatientMapper patientMapper;

    private final SchedulingProperties properties = new SchedulingProperties();
    private PatientServiceImpl patientService;

    @BeforeEach
    void setUp() {
        patientService = new PatientServiceImpl(patientRepository, patientMapper, properties);
    }

    private PatientRequest requestWithBirthDate(LocalDate birthDate) {
        return new PatientRequest("John Doe", UUID.randomUUID().toString(),
                "3001111", "john@mail.com", birthDate);
    }

    @Test
    void create_withValidBirthDate_persistsAndReturnsResponse() {
        PatientRequest request = requestWithBirthDate(LocalDate.of(1990, 5, 20));
        Patient entity = new Patient();
        Patient saved = new Patient();
        PatientResponse response = new PatientResponse(UUID.randomUUID(), "John Doe",
                request.document(), "3001111", "john@mail.com",
                request.birthDate(), Instant.now(), Instant.now());
        when(patientMapper.toEntity(request)).thenReturn(entity);
        when(patientRepository.save(entity)).thenReturn(saved);
        when(patientMapper.toResponse(saved)).thenReturn(response);

        assertThat(patientService.create(request)).isEqualTo(response);
        verify(patientRepository).save(entity);
    }

    @Test
    void create_withFutureBirthDate_throwsValidationAndDoesNotPersist() {
        PatientRequest request = requestWithBirthDate(LocalDate.now().plusDays(1));

        assertThatThrownBy(() -> patientService.create(request))
                .isInstanceOf(ValidationException.class);
        verify(patientRepository, never()).save(any());
    }

    @Test
    void create_withImplausibleAge_throwsValidation() {
        PatientRequest request = requestWithBirthDate(
                LocalDate.now().minusYears(properties.getMaxPatientAgeYears() + 1L));

        assertThatThrownBy(() -> patientService.create(request))
                .isInstanceOf(ValidationException.class);
        verify(patientRepository, never()).save(any());
    }

    @Test
    void create_withNullBirthDate_isAllowed() {
        PatientRequest request = requestWithBirthDate(null);
        Patient entity = new Patient();
        Patient saved = new Patient();
        PatientResponse response = new PatientResponse(UUID.randomUUID(), "John Doe",
                request.document(), "3001111", "john@mail.com", null, Instant.now(), Instant.now());
        when(patientMapper.toEntity(request)).thenReturn(entity);
        when(patientRepository.save(entity)).thenReturn(saved);
        when(patientMapper.toResponse(saved)).thenReturn(response);

        assertThat(patientService.create(request)).isEqualTo(response);
        verify(patientRepository).save(entity);
    }

    @Test
    void create_withExistingDocument_throwsConflictAndDoesNotPersist() {
        PatientRequest request = requestWithBirthDate(LocalDate.of(1990, 5, 20));
        when(patientRepository.existsByDocument(request.document())).thenReturn(true);

        assertThatThrownBy(() -> patientService.create(request))
                .isInstanceOf(ConflictException.class);
        verify(patientRepository, never()).save(any());
    }

    @Test
    void create_withExistingEmail_throwsConflictAndDoesNotPersist() {
        PatientRequest request = requestWithBirthDate(LocalDate.of(1990, 5, 20));
        when(patientRepository.existsByEmail(request.email())).thenReturn(true);

        assertThatThrownBy(() -> patientService.create(request))
                .isInstanceOf(ConflictException.class);
        verify(patientRepository, never()).save(any());
    }
}
