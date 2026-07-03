package com.camel.medisalud.service.impl;

import com.camel.medisalud.repository.PenaltyRepository;
import com.camel.medisalud.service.interfaces.PenaltyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PenaltyServiceImpl implements PenaltyService {

    private final PenaltyRepository penaltyRepository;
}
