package com.camel.medisalud.service.scheduling;

import com.camel.medisalud.config.SchedulingProperties;
import com.camel.medisalud.domain.enums.AppointmentStatus;
import com.camel.medisalud.domain.exception.ConflictException;
import com.camel.medisalud.domain.exception.ValidationException;
import com.camel.medisalud.domain.model.Appointment;
import com.camel.medisalud.domain.model.Doctor;
import com.camel.medisalud.domain.model.Patient;
import com.camel.medisalud.repository.AppointmentRepository;
import com.camel.medisalud.repository.PenaltyRepository;
import com.camel.medisalud.specification.AppointmentSpecification;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AppointmentValidator {

    private final WorkingHoursPolicy workingHoursPolicy;
    private final AppointmentRepository appointmentRepository;
    private final PenaltyRepository penaltyRepository;
    private final SchedulingProperties properties;

    public void validateForReservation(Doctor doctor, Patient patient, Instant when) {
        requireFuture(when);
        workingHoursPolicy.validateSlot(when);
        validateBirthDate(patient);
        ensurePenaltyThresholdNotReached(patient.getId());
        ensureDoctorFree(doctor.getId(), when, null);
        ensurePatientFree(doctor.getId(), patient.getId(), when, null);
    }

    private void requireFuture(Instant when) {
        if (!when.isAfter(Instant.now())) {
            throw new ValidationException("Appointment must be scheduled in the future");
        }
    }

    private void validateBirthDate(Patient patient) {
        LocalDate birthDate = patient.getBirthDate();
        if (birthDate != null && birthDate.isAfter(LocalDate.now(properties.getZone()))) {
            throw new ValidationException("Patient birth date cannot be in the future");
        }
    }

    private void ensurePenaltyThresholdNotReached(UUID patientId) {
        Instant since = Instant.now().minus(Duration.ofDays(properties.getPenaltyWindowDays()));
        long recentPenalties = penaltyRepository.countByPatientIdAndCreatedAtAfter(patientId, since);
        if (recentPenalties >= properties.getPenaltyBlockThreshold()) {
            throw new ConflictException(
                    "Patient is blocked: " + recentPenalties + " penalties in the last "
                            + properties.getPenaltyWindowDays() + " days");
        }
    }

    private void ensureDoctorFree(UUID doctorId, Instant when, UUID excludeId) {
        Specification<Appointment> conflict = activeSlotConflict(
                AppointmentSpecification.forDoctor(doctorId), when, excludeId);
        if (appointmentRepository.exists(conflict)) {
            throw new ConflictException(
                    "The doctor already has an appointment at the selected time");
        }
    }

    private void ensurePatientFree(UUID doctorId, UUID patientId, Instant when, UUID excludeId) {
        Specification<Appointment> conflict = activeSlotConflict(
                AppointmentSpecification.forPatient(patientId)
                        .and(AppointmentSpecification.forDoctor(doctorId)),
                when, excludeId);
        if (appointmentRepository.exists(conflict)) {
            throw new ConflictException(
                    "The patient already has an appointment with this doctor at the selected time");
        }
    }

    private Specification<Appointment> activeSlotConflict(
            Specification<Appointment> owner, Instant when, UUID excludeId) {
        return Specification.allOf(
                owner,
                AppointmentSpecification.hasStatus(AppointmentStatus.PROGRAMADA),
                AppointmentSpecification.startingAt(when),
                AppointmentSpecification.excludingId(excludeId));
    }
}
