CREATE INDEX ix_appointments_doctor_datetime
    ON appointments (doctor_id, appointment_date_time);
DROP INDEX ix_appointments_doctor;

CREATE INDEX ix_penalties_patient_created
    ON penalties (patient_id, created_at);
DROP INDEX ix_penalties_patient;
