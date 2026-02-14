package leets.weeth.domain.user.domain.service;

import java.util.List;
import leets.weeth.domain.user.application.exception.CardinalNotFoundException;
import leets.weeth.domain.user.application.exception.DuplicateCardinalException;
import leets.weeth.domain.user.domain.entity.Cardinal;
import leets.weeth.domain.user.domain.entity.enums.CardinalStatus;
import leets.weeth.domain.user.domain.repository.CardinalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CardinalGetService {

    private final CardinalRepository cardinalRepository;

    public Cardinal findByAdminSide(Integer cardinal) {
        return cardinalRepository.findByCardinalNumber(cardinal)
                .orElseGet(() -> cardinalRepository.save(Cardinal.builder().cardinalNumber(cardinal).build()));
    }

    public Cardinal findByUserSide(Integer cardinal) {
        return cardinalRepository.findByCardinalNumber(cardinal)
                .orElseThrow(CardinalNotFoundException::new);
    }

    public Cardinal find(Integer year, Integer semester) {
        return cardinalRepository.findByYearAndSemester(year, semester)
                .orElseThrow(CardinalNotFoundException::new);
    }

    public Cardinal findById(long cardinalId) {
        return cardinalRepository.findById(cardinalId)
                .orElseThrow(CardinalNotFoundException::new);
    }

    public List<Cardinal> findAll() {
        return cardinalRepository.findAllByOrderByCardinalNumberAsc();
    }

    public List<Cardinal> findAllCardinalNumberDesc() {
        return cardinalRepository.findAllByOrderByCardinalNumberDesc();
    }

    public List<Cardinal> findInProgress() {
        return cardinalRepository.findAllByStatus(CardinalStatus.IN_PROGRESS);
    }

    public void validateCardinal(Integer cardinal) {
        if (cardinalRepository.findByCardinalNumber(cardinal).isPresent()) {
            throw new DuplicateCardinalException();
        }
    }
}
