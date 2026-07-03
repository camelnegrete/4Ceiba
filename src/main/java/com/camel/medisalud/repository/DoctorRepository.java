package com.camel.medisalud.repository;

import com.camel.medisalud.domain.model.Doctor;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface DoctorRepository
        extends JpaRepository<Doctor, UUID>, JpaSpecificationExecutor<Doctor> {
}
