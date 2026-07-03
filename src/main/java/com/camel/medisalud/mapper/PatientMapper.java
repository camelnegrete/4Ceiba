package com.camel.medisalud.mapper;

import com.camel.medisalud.domain.model.Patient;
import com.camel.medisalud.dto.request.PatientRequest;
import com.camel.medisalud.dto.response.PatientResponse;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = MapperCentralConfig.class)
public interface PatientMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Patient toEntity(PatientRequest request);

    PatientResponse toResponse(Patient patient);

    List<PatientResponse> toResponseList(List<Patient> patients);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(@MappingTarget Patient patient, PatientRequest request);
}
