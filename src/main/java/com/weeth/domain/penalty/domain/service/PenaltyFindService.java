package com.weeth.domain.penalty.domain.service;

import com.weeth.domain.penalty.domain.entity.Penalty;
import com.weeth.domain.penalty.domain.entity.enums.PenaltyType;
import com.weeth.domain.penalty.domain.repository.PenaltyRepository;
import com.weeth.domain.penalty.application.exception.PenaltyNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PenaltyFindService {

    private final PenaltyRepository penaltyRepository;

    public Penalty find(Long penaltyId){
        return penaltyRepository.findById(penaltyId)
                .orElseThrow(PenaltyNotFoundException::new);
    }

    public List<Penalty> findAllByUserIdAndCardinalId(Long userId, Long cardinalId){
        return penaltyRepository.findByUserIdAndCardinalIdOrderByIdDesc(userId, cardinalId);
    }

    public List<Penalty> findAll(){
        return penaltyRepository.findAll();
    }

    public Penalty getRelatedAutoPenalty(Penalty penalty) {
        return penaltyRepository
                .findFirstByUserAndCardinalAndPenaltyTypeAndCreatedAtAfterOrderByCreatedAtAsc(
                        penalty.getUser(),
                        penalty.getCardinal(),
                        PenaltyType.AUTO_PENALTY,
                        penalty.getCreatedAt()
                ).orElse(null);
    }

    public List<Penalty> findAllByCardinalId(Long cardinalId) {
        return penaltyRepository.findByCardinalIdOrderByIdDesc(cardinalId);
    }
}
