ALTER TABLE doctors
    ADD COLUMN full_name VARCHAR(150) NOT NULL,
    ADD COLUMN specialty VARCHAR(100) NOT NULL,
    ADD COLUMN phone     VARCHAR(30),
    ADD COLUMN email     VARCHAR(150);

ALTER TABLE patients
    ADD COLUMN full_name  VARCHAR(150) NOT NULL,
    ADD COLUMN document   VARCHAR(50)  NOT NULL,
    ADD COLUMN phone      VARCHAR(30),
    ADD COLUMN email      VARCHAR(150) NOT NULL,
    ADD COLUMN birth_date DATE         NOT NULL;

ALTER TABLE patients
    ADD CONSTRAINT uk_patients_document UNIQUE (document),
    ADD CONSTRAINT uk_patients_email UNIQUE (email);

ALTER TABLE appointments
    ADD COLUMN doctor_id             UUID        NOT NULL,
    ADD COLUMN patient_id            UUID        NOT NULL,
    ADD COLUMN appointment_date_time TIMESTAMPTZ NOT NULL,
    ADD COLUMN status                VARCHAR(20) NOT NULL,
    ADD COLUMN cancelled_at          TIMESTAMPTZ;

ALTER TABLE appointments
    ADD CONSTRAINT fk_appointments_doctor
        FOREIGN KEY (doctor_id) REFERENCES doctors (id),
    ADD CONSTRAINT fk_appointments_patient
        FOREIGN KEY (patient_id) REFERENCES patients (id),
    ADD CONSTRAINT ck_appointments_status
        CHECK (status IN ('PROGRAMADA', 'CANCELADA', 'ATENDIDA'));

CREATE INDEX ix_appointments_doctor ON appointments (doctor_id);
CREATE INDEX ix_appointments_patient ON appointments (patient_id);
CREATE INDEX ix_appointments_date_time ON appointments (appointment_date_time);

ALTER TABLE penalties
    ADD COLUMN patient_id     UUID        NOT NULL,
    ADD COLUMN appointment_id UUID        NOT NULL,
    ADD COLUMN reason         VARCHAR(30) NOT NULL;

ALTER TABLE penalties
    ADD CONSTRAINT fk_penalties_patient
        FOREIGN KEY (patient_id) REFERENCES patients (id),
    ADD CONSTRAINT fk_penalties_appointment
        FOREIGN KEY (appointment_id) REFERENCES appointments (id),

    ADD CONSTRAINT uk_penalties_appointment UNIQUE (appointment_id),
    ADD CONSTRAINT ck_penalties_reason
        CHECK (reason IN ('LATE_CANCELLATION'));

CREATE INDEX ix_penalties_patient ON penalties (patient_id);
