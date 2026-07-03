package com.camel.medisalud.service.impl;

import com.camel.medisalud.domain.enums.AppointmentStatus;
import com.camel.medisalud.domain.enums.PenaltyReason;
import com.camel.medisalud.domain.exception.ConflictException;
import com.camel.medisalud.domain.exception.ResourceNotFoundException;
import com.camel.medisalud.domain.exception.ValidationException;
import com.camel.medisalud.domain.model.Appointment;
import com.camel.medisalud.domain.model.Doctor;
import com.camel.medisalud.domain.model.Patient;
import com.camel.medisalud.domain.model.Penalty;
import com.camel.medisalud.dto.request.AppointmentRequest;
import com.camel.medisalud.dto.response.AppointmentResponse;
import com.camel.medisalud.mapper.AppointmentMapper;
import com.camel.medisalud.repository.AppointmentRepository;
import com.camel.medisalud.repository.DoctorRepository;
import com.camel.medisalud.repository.PatientRepository;
import com.camel.medisalud.repository.PenaltyRepository;
import com.camel.medisalud.service.interfaces.AppointmentService;
import com.camel.medisalud.service.scheduling.AppointmentValidator;
import com.camel.medisalud.service.scheduling.WorkingHoursPolicy;
import com.camel.medisalud.specification.AppointmentSpecification;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final PenaltyRepository penaltyRepository;
    private final AppointmentMapper appointmentMapper;
    private final AppointmentValidator appointmentValidator;
    private final WorkingHoursPolicy workingHoursPolicy;

    @Override
    public AppointmentResponse reserveAppointment(AppointmentRequest request) {
        Doctor doctor = getDoctorOrThrow(request.doctorId());
        Patient patient = getPatientOrThrow(request.patientId());

        appointmentValidator.validateForReservation(doctor, patient, request.appointmentDateTime());

        Appointment appointment = appointmentMapper.toEntity(request, doctor, patient);
        appointment.setStatus(AppointmentStatus.PROGRAMADA);
        Appointment saved = appointmentRepository.save(appointment);
        log.debug("Reserved appointment {} for doctor {} and patient {} at {}",
                saved.getId(), doctor.getId(), patient.getId(), saved.getAppointmentDateTime());
        return appointmentMapper.toResponse(saved);
    }

    @Override
    public AppointmentResponse cancelAppointment(UUID appointmentId) {
        Appointment appointment = getAppointmentOrThrow(appointmentId);
        requireScheduled(appointment);
        applyCancellation(appointment);
        log.debug("Cancelled appointment {}", appointment.getId());
        return appointmentMapper.toResponse(appointmentRepository.save(appointment));
    }

    @Override
    public AppointmentResponse rescheduleAppointment(UUID appointmentId, Instant newAppointmentDateTime) {
        Appointment original = getAppointmentOrThrow(appointmentId);
        requireScheduled(original);
        Doctor doctor = original.getDoctor();
        Patient patient = original.getPatient();

        applyCancellation(original);
        appointmentRepository.save(original);

        appointmentValidator.validateForReservation(doctor, patient, newAppointmentDateTime);
        Appointment rescheduled = new Appointment();
        rescheduled.setDoctor(doctor);
        rescheduled.setPatient(patient);
        rescheduled.setAppointmentDateTime(newAppointmentDateTime);
        rescheduled.setStatus(AppointmentStatus.PROGRAMADA);
        Appointment saved = appointmentRepository.save(rescheduled);
        log.debug("Rescheduled appointment {} -> new appointment {} at {}",
                original.getId(), saved.getId(), newAppointmentDateTime);
        return appointmentMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Instant> findAvailableSlots(UUID doctorId, LocalDate startDate, LocalDate endDate) {
        getDoctorOrThrow(doctorId);
        if (startDate.isAfter(endDate)) {
            throw new ValidationException("startDate must not be after endDate");
        }

        List<Instant> allSlots = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            allSlots.addAll(workingHoursPolicy.slotsFor(date));
        }
        if (allSlots.isEmpty()) {
            return List.of();
        }

        Set<Instant> taken = takenSlots(doctorId, startDate, endDate);
        Instant now = Instant.now();
        return allSlots.stream()
                .filter(slot -> slot.isAfter(now))
                .filter(slot -> !taken.contains(slot))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AppointmentResponse> findAppointments(
            UUID doctorId, UUID patientId, AppointmentStatus status,
            Instant from, Instant to, Pageable pageable) {
        Specification<Appointment> spec = Specification.allOf(
                AppointmentSpecification.forDoctor(doctorId),
                AppointmentSpecification.forPatient(patientId),
                AppointmentSpecification.hasStatus(status),
                AppointmentSpecification.scheduledBetween(from, to));
        return appointmentRepository.findAll(spec, pageable).map(appointmentMapper::toResponse);
    }

    private Set<Instant> takenSlots(UUID doctorId, LocalDate startDate, LocalDate endDate) {
        Specification<Appointment> spec = Specification.allOf(
                AppointmentSpecification.forDoctor(doctorId),
                AppointmentSpecification.hasStatus(AppointmentStatus.PROGRAMADA),
                AppointmentSpecification.scheduledBetween(
                        workingHoursPolicy.startOfDay(startDate), workingHoursPolicy.endOfDay(endDate)));
        return appointmentRepository.findAll(spec).stream()
                .map(Appointment::getAppointmentDateTime)
                .collect(Collectors.toSet());
    }

    private void requireScheduled(Appointment appointment) {
        if (appointment.getStatus() != AppointmentStatus.PROGRAMADA) {
            throw new ConflictException(
                    "Only scheduled appointments can be cancelled or rescheduled");
        }
    }

    private void applyCancellation(Appointment appointment) {
        Instant now = Instant.now();
        appointment.setStatus(AppointmentStatus.CANCELADA);
        appointment.setCancelledAt(now);
        if (workingHoursPolicy.isLateCancellation(appointment.getAppointmentDateTime(), now)) {
            registerLateCancellationPenalty(appointment);
        }
    }

    private void registerLateCancellationPenalty(Appointment appointment) {
        Penalty penalty = new Penalty();
        penalty.setPatient(appointment.getPatient());
        penalty.setAppointment(appointment);
        penalty.setReason(PenaltyReason.LATE_CANCELLATION);
        penaltyRepository.save(penalty);
        log.info("Late-cancellation penalty registered for patient {} (appointment {})",
                appointment.getPatient().getId(), appointment.getId());
    }

    private Doctor getDoctorOrThrow(UUID id) {
        return doctorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with id: " + id));
    }

    private Patient getPatientOrThrow(UUID id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id: " + id));
    }

    private Appointment getAppointmentOrThrow(UUID id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + id));
    }
}
