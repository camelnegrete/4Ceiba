package com.camel.medisalud.mapper;

import com.camel.medisalud.domain.model.Penalty;
import com.camel.medisalud.dto.response.PenaltyResponse;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(
        config = MapperCentralConfig.class,
        uses = {PatientMapper.class, AppointmentMapper.class}
)
public interface PenaltyMapper {

    PenaltyResponse toResponse(Penalty penalty);

    List<PenaltyResponse> toResponseList(List<Penalty> penalties);
}
