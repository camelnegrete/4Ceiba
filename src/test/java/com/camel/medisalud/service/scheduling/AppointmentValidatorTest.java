package com.camel.medisalud.service.scheduling;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.camel.medisalud.config.SchedulingProperties;
import com.camel.medisalud.domain.exception.ConflictException;
import com.camel.medisalud.domain.exception.ValidationException;
import com.camel.medisalud.domain.model.Doctor;
import com.camel.medisalud.domain.model.Patient;
import com.camel.medisalud.repository.AppointmentRepository;
import com.camel.medisalud.repository.PenaltyRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class AppointmentValidatorTest {

    @Mock
    private WorkingHoursPolicy workingHoursPolicy;
    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private PenaltyRepository penaltyRepository;

    private final SchedulingProperties properties = new SchedulingProperties();
    private AppointmentValidator validator;

    private final Instant future = Instant.now().plus(Duration.ofDays(3));
    private Doctor doctor;
    private Patient patient;

    @BeforeEach
    void setUp() {
        validator = new AppointmentValidator(
                workingHoursPolicy, appointmentRepository, penaltyRepository, properties);
        doctor = new Doctor();
        doctor.setId(UUID.randomUUID());
        patient = new Patient();
        patient.setId(UUID.randomUUID());
    }

    @Test
    void reservation_whenAllRulesPass_validatesWorkingHours() {
        when(appointmentRepository.exists(any(Specification.class))).thenReturn(false);

        assertThatCode(() -> validator.validateForReservation(doctor, patient, future))
                .doesNotThrowAnyException();

        verify(workingHoursPolicy).validateSlot(future);
    }

    @Test
    void reservation_whenDateInThePast_throwsValidationAndSkipsQueries() {
        Instant past = Instant.now().minus(Duration.ofDays(1));

        assertThatThrownBy(() -> validator.validateForReservation(doctor, patient, past))
                .isInstanceOf(ValidationException.class);

        verifyNoInteractions(appointmentRepository, penaltyRepository, workingHoursPolicy);
    }

    @Test
    void reservation_whenBirthDateInFuture_throwsValidation() {
        patient.setBirthDate(LocalDate.now().plusDays(1));

        assertThatThrownBy(() -> validator.validateForReservation(doctor, patient, future))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void reservation_whenNullBirthDate_isAllowed() {
        patient.setBirthDate(null);
        when(appointmentRepository.exists(any(Specification.class))).thenReturn(false);

        assertThatCode(() -> validator.validateForReservation(doctor, patient, future))
                .doesNotThrowAnyException();
    }

    @Test
    void reservation_whenPenaltyThresholdReached_throwsConflict() {
        when(penaltyRepository.countByPatientIdAndCreatedAtAfter(any(), any()))
                .thenReturn((long) properties.getPenaltyBlockThreshold());

        assertThatThrownBy(() -> validator.validateForReservation(doctor, patient, future))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("penalties");
    }

    @Test
    void reservation_whenDoctorBusy_throwsConflict() {
        when(appointmentRepository.exists(any(Specification.class))).thenReturn(true);

        assertThatThrownBy(() -> validator.validateForReservation(doctor, patient, future))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("doctor");
    }

    @Test
    void reservation_whenPatientBusyWithSameDoctor_throwsConflict() {

        when(appointmentRepository.exists(any(Specification.class))).thenReturn(false, true);

        assertThatThrownBy(() -> validator.validateForReservation(doctor, patient, future))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("patient");
    }

}
