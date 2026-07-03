package com.camel.medisalud.controller;

import com.camel.medisalud.domain.enums.AppointmentStatus;
import com.camel.medisalud.dto.request.AppointmentRequest;
import com.camel.medisalud.dto.request.RescheduleAppointmentRequest;
import com.camel.medisalud.dto.response.AppointmentResponse;
import com.camel.medisalud.dto.response.ErrorResponse;
import com.camel.medisalud.service.interfaces.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
@Tag(name = "Appointments", description = "Appointment scheduling operations")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Invalid request",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Resource not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "501", description = "Operation not implemented yet",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
})
public class AppointmentController {

    private final AppointmentService appointmentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Reserve a new appointment")
    public ResponseEntity<AppointmentResponse> reserve(@Valid @RequestBody AppointmentRequest request) {
        AppointmentResponse created = appointmentService.reserveAppointment(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PostMapping("/{id}/cancellation")
    @Operation(summary = "Cancel an appointment")
    public ResponseEntity<AppointmentResponse> cancel(@PathVariable UUID id) {
        return ResponseEntity.ok(appointmentService.cancelAppointment(id));
    }

    @PatchMapping("/{id}/schedule")
    @Operation(summary = "Reschedule an appointment")
    public ResponseEntity<AppointmentResponse> reschedule(
            @PathVariable UUID id,
            @Valid @RequestBody RescheduleAppointmentRequest request) {
        return ResponseEntity.ok(
                appointmentService.rescheduleAppointment(id, request.newAppointmentDateTime()));
    }

    @GetMapping("/available-slots")
    @Operation(summary = "Find a doctor's available 30-minute slots within a date range")
    public ResponseEntity<List<Instant>> findAvailableSlots(
            @RequestParam UUID doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(
                appointmentService.findAvailableSlots(doctorId, startDate, endDate));
    }

    @GetMapping
    @Operation(summary = "Search appointments by optional filters (paginated and sortable)")
    public ResponseEntity<PagedModel<AppointmentResponse>> findAppointments(
            @RequestParam(required = false) UUID doctorId,
            @RequestParam(required = false) UUID patientId,
            @RequestParam(required = false) AppointmentStatus status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @ParameterObject
            @PageableDefault(size = 20, sort = "appointmentDateTime", direction = Sort.Direction.ASC)
            Pageable pageable) {
        Page<AppointmentResponse> page =
                appointmentService.findAppointments(doctorId, patientId, status, from, to, pageable);
        return ResponseEntity.ok(new PagedModel<>(page));
    }
}
