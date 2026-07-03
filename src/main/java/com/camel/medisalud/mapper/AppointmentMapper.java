package com.camel.medisalud.mapper;

import com.camel.medisalud.domain.model.Appointment;
import com.camel.medisalud.domain.model.Doctor;
import com.camel.medisalud.domain.model.Patient;
import com.camel.medisalud.dto.request.AppointmentRequest;
import com.camel.medisalud.dto.response.AppointmentResponse;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(
        config = MapperCentralConfig.class,
        uses = {DoctorMapper.class, PatientMapper.class}
)
public interface AppointmentMapper {

    @Mapping(target = "doctor", source = "doctor")
    @Mapping(target = "patient", source = "patient")
    @Mapping(target = "appointmentDateTime", source = "request.appointmentDateTime")
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "cancelledAt", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Appointment toEntity(AppointmentRequest request, Doctor doctor, Patient patient);

    AppointmentResponse toResponse(Appointment appointment);

    List<AppointmentResponse> toResponseList(List<Appointment> appointments);
}
