package com.camel.medisalud.specification;

import com.camel.medisalud.domain.enums.AppointmentStatus;
import com.camel.medisalud.domain.model.Appointment;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class AppointmentSpecification {

    private static final String DOCTOR = "doctor";
    private static final String PATIENT = "patient";
    private static final String ID = "id";
    private static final String STATUS = "status";
    private static final String APPOINTMENT_DATE_TIME = "appointmentDateTime";
    private static final String CANCELLED_AT = "cancelledAt";

    private AppointmentSpecification() {
    }

    public static Specification<Appointment> forDoctor(UUID doctorId) {
        return (root, query, cb) -> doctorId == null
                ? null
                : cb.equal(root.get(DOCTOR).get(ID), doctorId);
    }

    public static Specification<Appointment> forPatient(UUID patientId) {
        return (root, query, cb) -> patientId == null
                ? null
                : cb.equal(root.get(PATIENT).get(ID), patientId);
    }

    public static Specification<Appointment> hasStatus(AppointmentStatus status) {
        return (root, query, cb) -> status == null
                ? null
                : cb.equal(root.get(STATUS), status);
    }

    public static Specification<Appointment> startingAt(Instant when) {
        return (root, query, cb) -> when == null
                ? null
                : cb.equal(root.get(APPOINTMENT_DATE_TIME), when);
    }

    public static Specification<Appointment> excludingId(UUID id) {
        return (root, query, cb) -> id == null
                ? null
                : cb.notEqual(root.get(ID), id);
    }

    public static Specification<Appointment> scheduledBetween(Instant from, Instant to) {
        return (root, query, cb) -> {
            if (from != null && to != null) {
                return cb.between(root.<Instant>get(APPOINTMENT_DATE_TIME), from, to);
            }
            if (from != null) {
                return cb.greaterThanOrEqualTo(root.<Instant>get(APPOINTMENT_DATE_TIME), from);
            }
            if (to != null) {
                return cb.lessThanOrEqualTo(root.<Instant>get(APPOINTMENT_DATE_TIME), to);
            }
            return null;
        };
    }

    public static Specification<Appointment> cancelled(Boolean cancelled) {
        return (root, query, cb) -> {
            if (cancelled == null) {
                return null;
            }
            return cancelled
                    ? cb.isNotNull(root.get(CANCELLED_AT))
                    : cb.isNull(root.get(CANCELLED_AT));
        };
    }
}
