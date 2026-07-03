package com.camel.medisalud.service.impl;

import com.camel.medisalud.domain.exception.ResourceNotFoundException;
import com.camel.medisalud.domain.model.Doctor;
import com.camel.medisalud.dto.request.DoctorRequest;
import com.camel.medisalud.dto.response.DoctorResponse;
import com.camel.medisalud.mapper.DoctorMapper;
import com.camel.medisalud.repository.DoctorRepository;
import com.camel.medisalud.service.interfaces.DoctorService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class DoctorServiceImpl implements DoctorService {

    private final DoctorRepository doctorRepository;
    private final DoctorMapper doctorMapper;

    @Override
    public DoctorResponse create(DoctorRequest request) {
        Doctor doctor = doctorMapper.toEntity(request);
        return doctorMapper.toResponse(doctorRepository.save(doctor));
    }

    @Override
    public DoctorResponse update(UUID id, DoctorRequest request) {
        Doctor doctor = getByIdOrThrow(id);
        doctorMapper.updateEntity(doctor, request);
        return doctorMapper.toResponse(doctorRepository.save(doctor));
    }

    @Override
    @Transactional(readOnly = true)
    public DoctorResponse findById(UUID id) {
        return doctorMapper.toResponse(getByIdOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DoctorResponse> findAll() {
        return doctorMapper.toResponseList(doctorRepository.findAll());
    }

    @Override
    public void delete(UUID id) {
        if (!doctorRepository.existsById(id)) {
            throw doctorNotFound(id);
        }
        doctorRepository.deleteById(id);
    }

    private Doctor getByIdOrThrow(UUID id) {
        return doctorRepository.findById(id).orElseThrow(() -> doctorNotFound(id));
    }

    private ResourceNotFoundException doctorNotFound(UUID id) {
        return new ResourceNotFoundException("Doctor not found with id: " + id);
    }
}
