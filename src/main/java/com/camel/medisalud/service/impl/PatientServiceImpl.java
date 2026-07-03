package com.camel.medisalud.service.impl;

import com.camel.medisalud.config.SchedulingProperties;
import com.camel.medisalud.domain.exception.ConflictException;
import com.camel.medisalud.domain.exception.ResourceNotFoundException;
import com.camel.medisalud.domain.exception.ValidationException;
import com.camel.medisalud.domain.model.Patient;
import com.camel.medisalud.dto.request.PatientRequest;
import com.camel.medisalud.dto.response.PatientResponse;
import com.camel.medisalud.mapper.PatientMapper;
import com.camel.medisalud.repository.PatientRepository;
import com.camel.medisalud.service.interfaces.PatientService;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class PatientServiceImpl implements PatientService {

    private final PatientRepository patientRepository;
    private final PatientMapper patientMapper;
    private final SchedulingProperties schedulingProperties;

    @Override
    public PatientResponse create(PatientRequest request) {
        validateBirthDate(request.birthDate());
        ensureUniqueForCreate(request);
        Patient patient = patientMapper.toEntity(request);
        return patientMapper.toResponse(patientRepository.save(patient));
    }

    @Override
    public PatientResponse update(UUID id, PatientRequest request) {
        validateBirthDate(request.birthDate());
        Patient patient = getByIdOrThrow(id);
        ensureUniqueForUpdate(request, id);
        patientMapper.updateEntity(patient, request);
        return patientMapper.toResponse(patientRepository.save(patient));
    }

    @Override
    @Transactional(readOnly = true)
    public PatientResponse findById(UUID id) {
        return patientMapper.toResponse(getByIdOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PatientResponse> findAll() {
        return patientMapper.toResponseList(patientRepository.findAll());
    }

    @Override
    public void delete(UUID id) {
        if (!patientRepository.existsById(id)) {
            throw patientNotFound(id);
        }
        patientRepository.deleteById(id);
    }

    private void ensureUniqueForCreate(PatientRequest request) {
        if (patientRepository.existsByDocument(request.document())) {
            throw documentAlreadyExists(request.document());
        }
        if (patientRepository.existsByEmail(request.email())) {
            throw emailAlreadyExists(request.email());
        }
    }

    private void ensureUniqueForUpdate(PatientRequest request, UUID id) {
        if (patientRepository.existsByDocumentAndIdNot(request.document(), id)) {
            throw documentAlreadyExists(request.document());
        }
        if (patientRepository.existsByEmailAndIdNot(request.email(), id)) {
            throw emailAlreadyExists(request.email());
        }
    }

    private ConflictException documentAlreadyExists(String document) {
        return new ConflictException("A patient with document '" + document + "' already exists");
    }

    private ConflictException emailAlreadyExists(String email) {
        return new ConflictException("A patient with email '" + email + "' already exists");
    }

    private void validateBirthDate(LocalDate birthDate) {
        if (birthDate == null) {
            return;
        }
        LocalDate today = LocalDate.now();
        if (!birthDate.isBefore(today)) {
            throw new ValidationException("Birth date must be in the past");
        }
        int age = Period.between(birthDate, today).getYears();
        if (age > schedulingProperties.getMaxPatientAgeYears()) {
            throw new ValidationException(
                    "Birth date is not valid: age exceeds " + schedulingProperties.getMaxPatientAgeYears());
        }
    }

    private Patient getByIdOrThrow(UUID id) {
        return patientRepository.findById(id).orElseThrow(() -> patientNotFound(id));
    }

    private ResourceNotFoundException patientNotFound(UUID id) {
        return new ResourceNotFoundException("Patient not found with id: " + id);
    }
}
