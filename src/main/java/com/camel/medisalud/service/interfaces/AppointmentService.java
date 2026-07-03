package com.camel.medisalud.service.interfaces;

import com.camel.medisalud.domain.enums.AppointmentStatus;
import com.camel.medisalud.dto.request.AppointmentRequest;
import com.camel.medisalud.dto.response.AppointmentResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AppointmentService {

    AppointmentResponse reserveAppointment(AppointmentRequest request);

    AppointmentResponse cancelAppointment(UUID appointmentId);

    AppointmentResponse rescheduleAppointment(UUID appointmentId, Instant newAppointmentDateTime);

    List<Instant> findAvailableSlots(UUID doctorId, LocalDate startDate, LocalDate endDate);

    Page<AppointmentResponse> findAppointments(
            UUID doctorId,
            UUID patientId,
            AppointmentStatus status,
            Instant from,
            Instant to,
            Pageable pageable);
}
