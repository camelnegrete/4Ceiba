package com.camel.medisalud.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.camel.medisalud.domain.exception.ResourceNotFoundException;
import com.camel.medisalud.domain.model.Doctor;
import com.camel.medisalud.dto.request.DoctorRequest;
import com.camel.medisalud.dto.response.DoctorResponse;
import com.camel.medisalud.mapper.DoctorMapper;
import com.camel.medisalud.repository.DoctorRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DoctorServiceImplTest {

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private DoctorMapper doctorMapper;

    @InjectMocks
    private DoctorServiceImpl doctorService;

    private DoctorRequest sampleRequest() {
        return new DoctorRequest("Dr. House", "Diagnostics", "3000000", "house@clinic.com");
    }

    private DoctorResponse sampleResponse(UUID id) {
        return new DoctorResponse(id, "Dr. House", "Diagnostics", "3000000",
                "house@clinic.com", Instant.now(), Instant.now());
    }

    @Test
    void create_persistsMappedEntityAndReturnsResponse() {
        DoctorRequest request = sampleRequest();
        Doctor entity = new Doctor();
        Doctor saved = new Doctor();
        DoctorResponse response = sampleResponse(UUID.randomUUID());
        when(doctorMapper.toEntity(request)).thenReturn(entity);
        when(doctorRepository.save(entity)).thenReturn(saved);
        when(doctorMapper.toResponse(saved)).thenReturn(response);

        assertThat(doctorService.create(request)).isEqualTo(response);
        verify(doctorRepository).save(entity);
    }

    @Test
    void update_whenExists_appliesPartialUpdateAndReturnsResponse() {
        UUID id = UUID.randomUUID();
        DoctorRequest request = sampleRequest();
        Doctor existing = new Doctor();
        DoctorResponse response = sampleResponse(id);
        when(doctorRepository.findById(id)).thenReturn(Optional.of(existing));
        when(doctorRepository.save(existing)).thenReturn(existing);
        when(doctorMapper.toResponse(existing)).thenReturn(response);

        assertThat(doctorService.update(id, request)).isEqualTo(response);
        verify(doctorMapper).updateEntity(existing, request);
        verify(doctorRepository).save(existing);
    }

    @Test
    void findAll_returnsMappedResponses() {
        Doctor doctor = new Doctor();
        DoctorResponse response = sampleResponse(UUID.randomUUID());
        when(doctorRepository.findAll()).thenReturn(java.util.List.of(doctor));
        when(doctorMapper.toResponseList(java.util.List.of(doctor)))
                .thenReturn(java.util.List.of(response));

        assertThat(doctorService.findAll()).containsExactly(response);
    }

    @Test
    void findById_whenExists_returnsResponse() {
        UUID id = UUID.randomUUID();
        Doctor doctor = new Doctor();
        DoctorResponse response = sampleResponse(id);
        when(doctorRepository.findById(id)).thenReturn(Optional.of(doctor));
        when(doctorMapper.toResponse(doctor)).thenReturn(response);

        assertThat(doctorService.findById(id)).isEqualTo(response);
    }

    @Test
    void findById_whenMissing_throwsNotFound() {
        UUID id = UUID.randomUUID();
        when(doctorRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> doctorService.findById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_whenMissing_throwsNotFound() {
        UUID id = UUID.randomUUID();
        when(doctorRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> doctorService.update(id, sampleRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(doctorRepository, never()).save(any());
    }

    @Test
    void delete_whenMissing_throwsNotFoundAndDoesNotDelete() {
        UUID id = UUID.randomUUID();
        when(doctorRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> doctorService.delete(id))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(doctorRepository, never()).deleteById(any());
    }

    @Test
    void delete_whenExists_deletes() {
        UUID id = UUID.randomUUID();
        when(doctorRepository.existsById(id)).thenReturn(true);

        doctorService.delete(id);

        verify(doctorRepository).deleteById(id);
    }
}
