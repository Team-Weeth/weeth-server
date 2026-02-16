package com.weeth.domain.penalty.domain.service;

import com.weeth.domain.penalty.domain.entity.Penalty;
import com.weeth.domain.penalty.domain.repository.PenaltyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PenaltySaveService {

    private final PenaltyRepository penaltyRepository;

    public void save(Penalty penalty){
        penaltyRepository.save(penalty);
    }

}
