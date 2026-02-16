package com.weeth.domain.penalty.domain.service;

import com.weeth.domain.penalty.domain.repository.PenaltyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PenaltyDeleteService {

    private final PenaltyRepository penaltyRepository;

    public void delete(Long penaltyId){
        penaltyRepository.deleteById(penaltyId);
    }

}
