package com.camel.medisalud.service.interfaces;

import com.camel.medisalud.dto.request.DoctorRequest;
import com.camel.medisalud.dto.response.DoctorResponse;
import java.util.List;
import java.util.UUID;

public interface DoctorService {

    DoctorResponse create(DoctorRequest request);

    DoctorResponse update(UUID id, DoctorRequest request);

    DoctorResponse findById(UUID id);

    List<DoctorResponse> findAll();

    void delete(UUID id);
}
