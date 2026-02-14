package leets.weeth.domain.penalty.domain.service;

import leets.weeth.domain.penalty.application.dto.PenaltyDTO;
import leets.weeth.domain.penalty.domain.entity.Penalty;
import org.springframework.stereotype.Service;

@Service
public class PenaltyUpdateService {

    public void update(Penalty penalty, PenaltyDTO.Update dto) {
        if (dto.penaltyDescription() != null && !dto.penaltyDescription().isBlank()) {
            penalty.update(dto.penaltyDescription());
        }
    }
}
