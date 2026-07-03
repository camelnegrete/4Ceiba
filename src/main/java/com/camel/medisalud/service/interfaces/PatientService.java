package com.camel.medisalud.service.interfaces;

import com.camel.medisalud.dto.request.PatientRequest;
import com.camel.medisalud.dto.response.PatientResponse;
import java.util.List;
import java.util.UUID;

public interface PatientService {

    PatientResponse create(PatientRequest request);

    PatientResponse update(UUID id, PatientRequest request);

    PatientResponse findById(UUID id);

    List<PatientResponse> findAll();

    void delete(UUID id);
}
