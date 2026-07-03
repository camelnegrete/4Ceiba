package com.camel.medisalud.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.camel.medisalud.TestcontainersConfiguration;
import com.camel.medisalud.domain.enums.AppointmentStatus;
import com.camel.medisalud.domain.enums.PenaltyReason;
import com.camel.medisalud.domain.model.Appointment;
import com.camel.medisalud.domain.model.Doctor;
import com.camel.medisalud.domain.model.Patient;
import com.camel.medisalud.domain.model.Penalty;
import com.camel.medisalud.repository.AppointmentRepository;
import com.camel.medisalud.repository.DoctorRepository;
import com.camel.medisalud.repository.PatientRepository;
import com.camel.medisalud.repository.PenaltyRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@Transactional
class AppointmentFlowIntegrationTest {

    private static final ZoneId ZONE = ZoneId.of("America/Bogota");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private PenaltyRepository penaltyRepository;

    private Instant workingSlot(int hour) {
        LocalDate date = LocalDate.now(ZONE).plusDays(7);
        while (date.getDayOfWeek() == DayOfWeek.SATURDAY
                || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            date = date.plusDays(1);
        }
        return date.atTime(hour, 0).atZone(ZONE).toInstant();
    }

    private Instant slotOn(DayOfWeek day, int hour) {
        return nextDate(day).atTime(hour, 0).atZone(ZONE).toInstant();
    }

    private LocalDate nextDate(DayOfWeek day) {
        LocalDate date = LocalDate.now(ZONE).plusDays(1);
        while (date.getDayOfWeek() != day) {
            date = date.plusDays(1);
        }
        return date;
    }

    private LocalDate futureWeekday() {
        LocalDate date = LocalDate.now(ZONE).plusDays(7);
        while (date.getDayOfWeek() == DayOfWeek.SATURDAY
                || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            date = date.plusDays(1);
        }
        return date;
    }

    private ResultActions availability(UUID doctorId, LocalDate start, LocalDate end) throws Exception {
        return mockMvc.perform(get("/api/v1/appointments/available-slots")
                .param("doctorId", doctorId.toString())
                .param("startDate", start.toString())
                .param("endDate", end.toString()));
    }

    private UUID createDoctor() throws Exception {
        String body = """
                {"fullName":"Dr. House","specialty":"Diagnostics",
                 "phone":"3000000","email":"house+%s@clinic.com"}
                """.formatted(UUID.randomUUID());
        return idOf(mockMvc.perform(post("/api/v1/doctors")
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()));
    }

    private UUID createPatient() throws Exception {
        String body = """
                {"fullName":"John Doe","document":"%s","phone":"3001111",
                 "email":"john+%s@mail.com","birthDate":"1990-05-20"}
                """.formatted(UUID.randomUUID(), UUID.randomUUID());
        return idOf(mockMvc.perform(post("/api/v1/patients")
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()));
    }

    private ResultActions reserve(UUID doctorId, UUID patientId, Instant when) throws Exception {
        String body = """
                {"doctorId":"%s","patientId":"%s","appointmentDateTime":"%s"}
                """.formatted(doctorId, patientId, when);
        return mockMvc.perform(post("/api/v1/appointments")
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions reschedule(UUID appointmentId, Instant newWhen) throws Exception {
        String body = """
                {"newAppointmentDateTime":"%s"}
                """.formatted(newWhen);
        return mockMvc.perform(patch("/api/v1/appointments/{id}/schedule", appointmentId)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private UUID idOf(ResultActions actions) throws Exception {
        String json = actions.andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(json).get("id").asText());
    }

    @Test
    void registerDoctor_returnsCreated() throws Exception {
        createDoctor();
    }

    @Test
    void registerPatient_returnsCreated() throws Exception {
        createPatient();
    }

    @Test
    void reserve_valid_returnsCreatedAndScheduled() throws Exception {
        UUID doctorId = createDoctor();
        UUID patientId = createPatient();

        reserve(doctorId, patientId, workingSlot(9))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PROGRAMADA"))
                .andExpect(jsonPath("$.doctor.id").value(doctorId.toString()))
                .andExpect(jsonPath("$.patient.id").value(patientId.toString()));
    }

    @Test
    void reserve_duplicateDoctorSlot_returnsConflict() throws Exception {
        UUID doctorId = createDoctor();
        UUID patientA = createPatient();
        UUID patientB = createPatient();
        Instant slot = workingSlot(9);

        reserve(doctorId, patientA, slot).andExpect(status().isCreated());
        reserve(doctorId, patientB, slot).andExpect(status().isConflict());
    }

    @Test
    void reserve_outsideWorkingHours_returnsBadRequest() throws Exception {
        UUID doctorId = createDoctor();
        UUID patientId = createPatient();

        reserve(doctorId, patientId, workingSlot(7))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reserve_onSaturdayMorning_returnsCreated() throws Exception {
        UUID doctorId = createDoctor();
        UUID patientId = createPatient();

        reserve(doctorId, patientId, slotOn(DayOfWeek.SATURDAY, 9))
                .andExpect(status().isCreated());
    }

    @Test
    void reserve_onSaturdayAfternoon_returnsBadRequest() throws Exception {
        UUID doctorId = createDoctor();
        UUID patientId = createPatient();

        reserve(doctorId, patientId, slotOn(DayOfWeek.SATURDAY, 14))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reserve_onSunday_returnsBadRequest() throws Exception {
        UUID doctorId = createDoctor();
        UUID patientId = createPatient();

        reserve(doctorId, patientId, slotOn(DayOfWeek.SUNDAY, 9))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reserve_whenPatientHasThreeRecentPenalties_returnsConflict() throws Exception {
        Doctor doctor = persistDoctor();
        Patient patient = persistPatient();
        seedPenalties(doctor, patient, 3);

        reserve(doctor.getId(), patient.getId(), workingSlot(9))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void cancel_normal_returnsCancelledAndCreatesNoPenalty() throws Exception {
        UUID doctorId = createDoctor();
        UUID patientId = createPatient();

        UUID appointmentId = idOf(reserve(doctorId, patientId, workingSlot(9))
                .andExpect(status().isCreated()));

        mockMvc.perform(post("/api/v1/appointments/{id}/cancellation", appointmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADA"))
                .andExpect(jsonPath("$.cancelledAt").isNotEmpty());

        assertThat(penaltyRepository.findAll()).isEmpty();
    }

    @Test
    void cancel_lateWithinTwoHours_persistsPenaltyLinkedToPatientAndAppointment() throws Exception {
        Doctor doctor = persistDoctor();
        Patient patient = persistPatient();

        Appointment appointment = seedScheduledAppointment(
                doctor, patient, Instant.now().plus(Duration.ofHours(1)));

        mockMvc.perform(post("/api/v1/appointments/{id}/cancellation", appointment.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADA"));

        List<Penalty> penalties = penaltyRepository.findAll();
        assertThat(penalties).hasSize(1);
        assertThat(penalties.get(0).getReason()).isEqualTo(PenaltyReason.LATE_CANCELLATION);
        assertThat(penalties.get(0).getPatient().getId()).isEqualTo(patient.getId());
        assertThat(penalties.get(0).getAppointment().getId()).isEqualTo(appointment.getId());
    }

    @Test
    void cancel_nonExistentAppointment_returnsNotFound() throws Exception {
        mockMvc.perform(post("/api/v1/appointments/{id}/cancellation", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void cancel_alreadyCancelled_returnsConflict() throws Exception {
        UUID doctorId = createDoctor();
        UUID patientId = createPatient();
        UUID appointmentId = idOf(reserve(doctorId, patientId, workingSlot(9))
                .andExpect(status().isCreated()));

        mockMvc.perform(post("/api/v1/appointments/{id}/cancellation", appointmentId))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/appointments/{id}/cancellation", appointmentId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void reschedule_movesToNewSlotAndCancelsOriginal() throws Exception {
        UUID doctorId = createDoctor();
        UUID patientId = createPatient();
        UUID originalId = idOf(reserve(doctorId, patientId, workingSlot(9))
                .andExpect(status().isCreated()));
        Instant newSlot = workingSlot(10);

        UUID newId = idOf(reschedule(originalId, newSlot)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROGRAMADA"))
                .andExpect(jsonPath("$.appointmentDateTime").value(newSlot.toString())));

        assertThat(newId).isNotEqualTo(originalId);
        assertThat(appointmentRepository.findById(originalId).orElseThrow().getStatus())
                .isEqualTo(AppointmentStatus.CANCELADA);
        assertThat(appointmentRepository.findById(newId).orElseThrow().getStatus())
                .isEqualTo(AppointmentStatus.PROGRAMADA);
        assertThat(penaltyRepository.findAll()).isEmpty();
    }

    @Test
    void reschedule_whenOriginalLate_createsPenalty() throws Exception {
        Doctor doctor = persistDoctor();
        Patient patient = persistPatient();
        Appointment original = seedScheduledAppointment(
                doctor, patient, Instant.now().plus(Duration.ofHours(1)));

        reschedule(original.getId(), workingSlot(9))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROGRAMADA"));

        assertThat(appointmentRepository.findById(original.getId()).orElseThrow().getStatus())
                .isEqualTo(AppointmentStatus.CANCELADA);
        assertThat(penaltyRepository.findAll()).hasSize(1);
    }

    @Test
    void reschedule_toConflictingSlot_returnsConflict() throws Exception {
        UUID doctorId = createDoctor();
        UUID patientA = createPatient();
        UUID patientB = createPatient();

        reserve(doctorId, patientA, workingSlot(10)).andExpect(status().isCreated());
        UUID originalId = idOf(reserve(doctorId, patientB, workingSlot(9))
                .andExpect(status().isCreated()));

        reschedule(originalId, workingSlot(10))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void reschedule_nonExistentAppointment_returnsNotFound() throws Exception {
        reschedule(UUID.randomUUID(), workingSlot(9))
                .andExpect(status().isNotFound());
    }

    @Test
    void findAppointments_byDoctor_returnsOnlyThatDoctor() throws Exception {
        UUID doctorA = createDoctor();
        UUID doctorB = createDoctor();
        UUID patient = createPatient();
        reserve(doctorA, patient, workingSlot(9)).andExpect(status().isCreated());
        reserve(doctorB, patient, workingSlot(10)).andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/appointments").param("doctorId", doctorA.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].doctor.id").value(doctorA.toString()));
    }

    @Test
    void findAppointments_byPatient_returnsOnlyThatPatient() throws Exception {
        UUID doctor = createDoctor();
        UUID patientA = createPatient();
        UUID patientB = createPatient();
        reserve(doctor, patientA, workingSlot(9)).andExpect(status().isCreated());
        reserve(doctor, patientB, workingSlot(10)).andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/appointments").param("patientId", patientA.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].patient.id").value(patientA.toString()));
    }

    @Test
    void findAppointments_byStatus_returnsOnlyMatchingStatus() throws Exception {
        UUID doctor = createDoctor();
        UUID patient = createPatient();
        UUID toCancel = idOf(reserve(doctor, patient, workingSlot(9))
                .andExpect(status().isCreated()));
        reserve(doctor, patient, workingSlot(10)).andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/appointments/{id}/cancellation", toCancel))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/appointments")
                .param("doctorId", doctor.toString())
                .param("status", "CANCELADA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].status").value("CANCELADA"));
    }

    @Test
    void findAppointments_byDateRange_returnsSlotsWithinRange() throws Exception {
        UUID doctor = createDoctor();
        UUID patient = createPatient();
        Instant slot = workingSlot(9);
        reserve(doctor, patient, slot).andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/appointments")
                .param("doctorId", doctor.toString())
                .param("from", slot.minus(Duration.ofHours(1)).toString())
                .param("to", slot.plus(Duration.ofHours(1)).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));

        mockMvc.perform(get("/api/v1/appointments")
                .param("doctorId", doctor.toString())
                .param("from", slot.plus(Duration.ofDays(1)).toString())
                .param("to", slot.plus(Duration.ofDays(2)).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void findAppointments_paginated_limitsPageSizeAndReportsTotal() throws Exception {
        UUID doctor = createDoctor();
        UUID patient = createPatient();
        reserve(doctor, patient, workingSlot(9)).andExpect(status().isCreated());
        reserve(doctor, patient, workingSlot(10)).andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/appointments")
                .param("doctorId", doctor.toString())
                .param("size", "1")
                .param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.page.totalElements").value(2))
                .andExpect(jsonPath("$.page.size").value(1));
    }

    @Test
    void findAppointments_sortedByDateDesc_returnsLatestFirst() throws Exception {
        UUID doctor = createDoctor();
        UUID patient = createPatient();
        Instant earlier = workingSlot(9);
        Instant later = workingSlot(10);
        reserve(doctor, patient, earlier).andExpect(status().isCreated());
        reserve(doctor, patient, later).andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/appointments")
                .param("doctorId", doctor.toString())
                .param("sort", "appointmentDateTime,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].appointmentDateTime").value(later.toString()))
                .andExpect(jsonPath("$.content[1].appointmentDateTime").value(earlier.toString()));
    }

    @Test
    void availability_fullWeekday_returnsAllTwentySlots() throws Exception {
        UUID doctorId = createDoctor();
        LocalDate day = futureWeekday();

        availability(doctorId, day, day)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(20));
    }

    @Test
    void availability_doctorWithoutAppointments_returnsSlots() throws Exception {
        UUID doctorId = createDoctor();
        LocalDate day = futureWeekday();

        availability(doctorId, day, day)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").isNotEmpty());
    }

    @Test
    void availability_doctorWithAppointment_excludesBookedSlot() throws Exception {
        UUID doctorId = createDoctor();
        UUID patientId = createPatient();
        LocalDate day = futureWeekday();
        Instant slot = day.atTime(9, 0).atZone(ZONE).toInstant();
        reserve(doctorId, patientId, slot).andExpect(status().isCreated());

        availability(doctorId, day, day)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(19));
    }

    @Test
    void availability_saturday_returnsTenSlots() throws Exception {
        UUID doctorId = createDoctor();
        LocalDate saturday = nextDate(DayOfWeek.SATURDAY);

        availability(doctorId, saturday, saturday)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(10));
    }

    @Test
    void availability_sunday_returnsNoSlots() throws Exception {
        UUID doctorId = createDoctor();
        LocalDate sunday = nextDate(DayOfWeek.SUNDAY);

        availability(doctorId, sunday, sunday)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    private Doctor persistDoctor() {
        Doctor doctor = new Doctor();
        doctor.setFullName("Dr. House");
        doctor.setSpecialty("Diagnostics");
        doctor.setPhone("3000000");
        doctor.setEmail("house+" + UUID.randomUUID() + "@clinic.com");
        return doctorRepository.save(doctor);
    }

    private Patient persistPatient() {
        Patient patient = new Patient();
        patient.setFullName("John Doe");
        patient.setDocument(UUID.randomUUID().toString());
        patient.setPhone("3001111");
        patient.setEmail("john+" + UUID.randomUUID() + "@mail.com");
        patient.setBirthDate(LocalDate.of(1990, 5, 20));
        return patientRepository.save(patient);
    }

    private Appointment seedScheduledAppointment(Doctor doctor, Patient patient, Instant when) {
        Appointment appointment = new Appointment();
        appointment.setDoctor(doctor);
        appointment.setPatient(patient);
        appointment.setAppointmentDateTime(when);
        appointment.setStatus(AppointmentStatus.PROGRAMADA);
        return appointmentRepository.save(appointment);
    }

    private void seedPenalties(Doctor doctor, Patient patient, int count) {
        for (int i = 0; i < count; i++) {
            Appointment appointment = new Appointment();
            appointment.setDoctor(doctor);
            appointment.setPatient(patient);
            appointment.setAppointmentDateTime(Instant.now().minus(Duration.ofDays(i + 1L)));
            appointment.setStatus(AppointmentStatus.CANCELADA);
            appointment = appointmentRepository.save(appointment);

            Penalty penalty = new Penalty();
            penalty.setPatient(patient);
            penalty.setAppointment(appointment);
            penalty.setReason(PenaltyReason.LATE_CANCELLATION);
            penaltyRepository.save(penalty);
        }
    }
}
