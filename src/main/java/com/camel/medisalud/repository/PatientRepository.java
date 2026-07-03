package com.camel.medisalud.repository;

import com.camel.medisalud.domain.model.Patient;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface PatientRepository
        extends JpaRepository<Patient, UUID>, JpaSpecificationExecutor<Patient> {

    boolean existsByDocument(String document);

    boolean existsByEmail(String email);

    boolean existsByDocumentAndIdNot(String document, UUID id);

    boolean existsByEmailAndIdNot(String email, UUID id);
}
