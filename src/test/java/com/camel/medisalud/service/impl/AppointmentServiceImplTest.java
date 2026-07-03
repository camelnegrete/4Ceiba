package com.camel.medisalud.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.camel.medisalud.domain.enums.AppointmentStatus;
import com.camel.medisalud.domain.enums.PenaltyReason;
import com.camel.medisalud.domain.exception.ConflictException;
import com.camel.medisalud.domain.exception.ResourceNotFoundException;
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
import com.camel.medisalud.service.scheduling.AppointmentValidator;
import com.camel.medisalud.service.scheduling.WorkingHoursPolicy;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceImplTest {

    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private DoctorRepository doctorRepository;
    @Mock
    private PatientRepository patientRepository;
    @Mock
    private PenaltyRepository penaltyRepository;
    @Mock
    private AppointmentMapper appointmentMapper;
    @Mock
    private AppointmentValidator appointmentValidator;
    @Mock
    private WorkingHoursPolicy workingHoursPolicy;

    @InjectMocks
    private AppointmentServiceImpl appointmentService;

    private final Instant future = Instant.now().plus(Duration.ofDays(3));
    private UUID doctorId;
    private UUID patientId;
    private Doctor doctor;
    private Patient patient;

    @BeforeEach
    void setUp() {
        doctorId = UUID.randomUUID();
        patientId = UUID.randomUUID();
        doctor = new Doctor();
        doctor.setId(doctorId);
        patient = new Patient();
        patient.setId(patientId);
    }

    private AppointmentResponse anyResponse() {
        return new AppointmentResponse(UUID.randomUUID(), null, null, future,
                AppointmentStatus.PROGRAMADA, null, null, null);
    }

    @Test
    void reserve_valid_setsScheduledStatusAndPersists() {
        AppointmentRequest request = new AppointmentRequest(doctorId, patientId, future);
        Appointment entity = new Appointment();
        AppointmentResponse response = anyResponse();
        when(doctorRepository.findById(doctorId)).thenReturn(Optional.of(doctor));
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(appointmentMapper.toEntity(request, doctor, patient)).thenReturn(entity);
        when(appointmentRepository.save(entity)).thenReturn(entity);
        when(appointmentMapper.toResponse(entity)).thenReturn(response);

        assertThat(appointmentService.reserveAppointment(request)).isEqualTo(response);
        verify(appointmentValidator).validateForReservation(doctor, patient, future);
        assertThat(entity.getStatus()).isEqualTo(AppointmentStatus.PROGRAMADA);
    }

    @Test
    void reserve_whenDoctorMissing_throwsNotFoundAndSkipsValidation() {
        AppointmentRequest request = new AppointmentRequest(doctorId, patientId, future);
        when(doctorRepository.findById(doctorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appointmentService.reserveAppointment(request))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(appointmentValidator);
    }

    @Test
    void reserve_whenSlotConflicts_propagatesAndDoesNotPersist() {
        AppointmentRequest request = new AppointmentRequest(doctorId, patientId, future);
        when(doctorRepository.findById(doctorId)).thenReturn(Optional.of(doctor));
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        doThrow(new ConflictException("busy"))
                .when(appointmentValidator).validateForReservation(doctor, patient, future);

        assertThatThrownBy(() -> appointmentService.reserveAppointment(request))
                .isInstanceOf(ConflictException.class);
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void cancel_whenNotLate_cancelsWithoutPenalty() {
        UUID id = UUID.randomUUID();
        Appointment appointment = scheduledAppointment();
        when(appointmentRepository.findById(id)).thenReturn(Optional.of(appointment));
        when(workingHoursPolicy.isLateCancellation(eq(future), any())).thenReturn(false);
        when(appointmentRepository.save(appointment)).thenReturn(appointment);
        when(appointmentMapper.toResponse(appointment)).thenReturn(anyResponse());

        appointmentService.cancelAppointment(id);

        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CANCELADA);
        assertThat(appointment.getCancelledAt()).isNotNull();
        verify(penaltyRepository, never()).save(any());
    }

    @Test
    void cancel_whenLate_createsLateCancellationPenalty() {
        UUID id = UUID.randomUUID();
        Appointment appointment = scheduledAppointment();
        when(appointmentRepository.findById(id)).thenReturn(Optional.of(appointment));
        when(workingHoursPolicy.isLateCancellation(eq(future), any())).thenReturn(true);
        when(appointmentRepository.save(appointment)).thenReturn(appointment);
        when(appointmentMapper.toResponse(appointment)).thenReturn(anyResponse());

        appointmentService.cancelAppointment(id);

        ArgumentCaptor<Penalty> captor = ArgumentCaptor.forClass(Penalty.class);
        verify(penaltyRepository).save(captor.capture());
        assertThat(captor.getValue().getReason()).isEqualTo(PenaltyReason.LATE_CANCELLATION);
        assertThat(captor.getValue().getPatient()).isEqualTo(patient);
        assertThat(captor.getValue().getAppointment()).isEqualTo(appointment);
    }

    @Test
    void cancel_whenAlreadyCancelled_throwsConflict() {
        UUID id = UUID.randomUUID();
        Appointment appointment = scheduledAppointment();
        appointment.setStatus(AppointmentStatus.CANCELADA);
        when(appointmentRepository.findById(id)).thenReturn(Optional.of(appointment));

        assertThatThrownBy(() -> appointmentService.cancelAppointment(id))
                .isInstanceOf(ConflictException.class);
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void cancel_whenAppointmentMissing_throwsNotFound() {
        UUID id = UUID.randomUUID();
        when(appointmentRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appointmentService.cancelAppointment(id))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(appointmentRepository, never()).save(any());
        verify(penaltyRepository, never()).save(any());
    }

    @Test
    void reschedule_cancelsOriginalAndCreatesNewValidatedAppointment() {
        UUID id = UUID.randomUUID();
        Instant newSlot = future.plus(Duration.ofDays(1));
        Appointment original = scheduledAppointment();
        when(appointmentRepository.findById(id)).thenReturn(Optional.of(original));
        when(workingHoursPolicy.isLateCancellation(eq(future), any())).thenReturn(false);
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(appointmentMapper.toResponse(any(Appointment.class))).thenReturn(anyResponse());

        appointmentService.rescheduleAppointment(id, newSlot);

        assertThat(original.getStatus()).isEqualTo(AppointmentStatus.CANCELADA);
        assertThat(original.getCancelledAt()).isNotNull();
        verify(appointmentValidator).validateForReservation(doctor, patient, newSlot);

        ArgumentCaptor<Appointment> captor = ArgumentCaptor.forClass(Appointment.class);
        verify(appointmentRepository, times(2)).save(captor.capture());
        Appointment created = captor.getAllValues().get(1);
        assertThat(created.getStatus()).isEqualTo(AppointmentStatus.PROGRAMADA);
        assertThat(created.getAppointmentDateTime()).isEqualTo(newSlot);
        assertThat(created.getDoctor()).isEqualTo(doctor);
        assertThat(created.getPatient()).isEqualTo(patient);
    }

    @Test
    void reschedule_whenOriginalLate_penalisesBeforeCreatingNew() {
        UUID id = UUID.randomUUID();
        Instant newSlot = future.plus(Duration.ofDays(1));
        Appointment original = scheduledAppointment();
        when(appointmentRepository.findById(id)).thenReturn(Optional.of(original));
        when(workingHoursPolicy.isLateCancellation(eq(future), any())).thenReturn(true);
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(appointmentMapper.toResponse(any(Appointment.class))).thenReturn(anyResponse());

        appointmentService.rescheduleAppointment(id, newSlot);

        verify(penaltyRepository).save(any(Penalty.class));
        verify(appointmentValidator).validateForReservation(doctor, patient, newSlot);
    }

    @Test
    void reschedule_whenOriginalNotScheduled_throwsConflict() {
        UUID id = UUID.randomUUID();
        Appointment original = scheduledAppointment();
        original.setStatus(AppointmentStatus.CANCELADA);
        when(appointmentRepository.findById(id)).thenReturn(Optional.of(original));

        assertThatThrownBy(() -> appointmentService.rescheduleAppointment(id, future))
                .isInstanceOf(ConflictException.class);
        verify(appointmentRepository, never()).save(any());
        verifyNoInteractions(appointmentValidator);
    }

    @Test
    void reschedule_whenMissing_throwsNotFound() {
        UUID id = UUID.randomUUID();
        when(appointmentRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appointmentService.rescheduleAppointment(id, future))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void reschedule_whenNewSlotInvalid_propagatesConflict() {
        UUID id = UUID.randomUUID();
        Appointment original = scheduledAppointment();
        when(appointmentRepository.findById(id)).thenReturn(Optional.of(original));
        when(workingHoursPolicy.isLateCancellation(eq(future), any())).thenReturn(false);
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new ConflictException("busy"))
                .when(appointmentValidator).validateForReservation(doctor, patient, future);

        assertThatThrownBy(() -> appointmentService.rescheduleAppointment(id, future))
                .isInstanceOf(ConflictException.class);

        verify(appointmentRepository, times(1)).save(any(Appointment.class));
    }

    @Test
    void findAppointments_delegatesToRepositoryWithSpecificationAndPageable() {
        Appointment appointment = scheduledAppointment();
        AppointmentResponse response = anyResponse();
        Pageable pageable = PageRequest.of(0, 20);
        when(appointmentRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(appointment), pageable, 1));
        when(appointmentMapper.toResponse(appointment)).thenReturn(response);

        Page<AppointmentResponse> result = appointmentService.findAppointments(
                doctorId, null, AppointmentStatus.PROGRAMADA, null, null, pageable);

        assertThat(result.getContent()).containsExactly(response);
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(appointmentRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void findAvailableSlots_excludesTakenAndPastSlots() {
        LocalDate date = LocalDate.now();
        Instant pastSlot = Instant.now().minus(Duration.ofHours(1));
        Instant takenSlot = Instant.now().plus(Duration.ofDays(1));
        Instant freeSlot = Instant.now().plus(Duration.ofDays(1)).plus(Duration.ofMinutes(30));
        when(doctorRepository.findById(doctorId)).thenReturn(Optional.of(doctor));
        when(workingHoursPolicy.slotsFor(date)).thenReturn(List.of(pastSlot, takenSlot, freeSlot));
        Appointment taken = new Appointment();
        taken.setAppointmentDateTime(takenSlot);
        when(appointmentRepository.findAll(any(Specification.class))).thenReturn(List.of(taken));

        List<Instant> result = appointmentService.findAvailableSlots(doctorId, date, date);

        assertThat(result).containsExactly(freeSlot);
    }

    @Test
    void findAvailableSlots_withNoAppointments_returnsAllFutureSlots() {
        LocalDate date = LocalDate.now();
        Instant slotOne = Instant.now().plus(Duration.ofDays(1));
        Instant slotTwo = slotOne.plus(Duration.ofMinutes(30));
        when(doctorRepository.findById(doctorId)).thenReturn(Optional.of(doctor));
        when(workingHoursPolicy.slotsFor(date)).thenReturn(List.of(slotOne, slotTwo));
        when(appointmentRepository.findAll(any(Specification.class))).thenReturn(List.of());

        assertThat(appointmentService.findAvailableSlots(doctorId, date, date))
                .containsExactly(slotOne, slotTwo);
    }

    @Test
    void findAvailableSlots_whenStartAfterEnd_throwsValidation() {
        LocalDate start = LocalDate.now().plusDays(5);
        LocalDate end = LocalDate.now().plusDays(1);
        when(doctorRepository.findById(doctorId)).thenReturn(Optional.of(doctor));

        assertThatThrownBy(() -> appointmentService.findAvailableSlots(doctorId, start, end))
                .isInstanceOf(com.camel.medisalud.domain.exception.ValidationException.class);
    }

    private Appointment scheduledAppointment() {
        Appointment appointment = new Appointment();
        appointment.setId(UUID.randomUUID());
        appointment.setStatus(AppointmentStatus.PROGRAMADA);
        appointment.setDoctor(doctor);
        appointment.setPatient(patient);
        appointment.setAppointmentDateTime(future);
        return appointment;
    }
}
