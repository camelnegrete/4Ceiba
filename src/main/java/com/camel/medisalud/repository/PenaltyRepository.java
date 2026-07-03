package com.camel.medisalud.repository;

import com.camel.medisalud.domain.model.Penalty;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface PenaltyRepository
        extends JpaRepository<Penalty, UUID>, JpaSpecificationExecutor<Penalty> {

    long countByPatientIdAndCreatedAtAfter(UUID patientId, Instant since);
}
