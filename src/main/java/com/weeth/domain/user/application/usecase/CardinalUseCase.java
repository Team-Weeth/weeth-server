package com.weeth.domain.user.application.usecase;

import com.weeth.domain.user.application.dto.request.CardinalSaveRequest;
import com.weeth.domain.user.application.dto.request.CardinalUpdateRequest;
import com.weeth.domain.user.application.dto.response.CardinalResponse;
import com.weeth.domain.user.application.mapper.CardinalMapper;
import com.weeth.domain.user.domain.entity.Cardinal;
import com.weeth.domain.user.domain.service.CardinalGetService;
import com.weeth.domain.user.domain.service.CardinalSaveService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CardinalUseCase {

    private final CardinalGetService cardinalGetService;
    private final CardinalSaveService cardinalSaveService;

    private final CardinalMapper cardinalMapper;

    @Transactional
    public void save(CardinalSaveRequest dto) {
        cardinalGetService.validateCardinal(dto.cardinalNumber());

        Cardinal cardinal = cardinalSaveService.save(cardinalMapper.from(dto));

        if (dto.inProgress()) {
            updateCardinalStatus(cardinal);
        }
    }

    @Transactional
    public void update(CardinalUpdateRequest dto) {
        Cardinal cardinal = cardinalGetService.findById(dto.id());

        cardinal.update(dto);

        if (dto.inProgress()) {
            updateCardinalStatus(cardinal);
        }
    }

    public List<CardinalResponse> findAll() {
        List<Cardinal> cardinals = cardinalGetService.findAll();
        return cardinals.stream()
                .map(cardinalMapper::to)
                .toList();
    }

    private void updateCardinalStatus(Cardinal cardinal) {
        List<Cardinal> cardinals = cardinalGetService.findInProgress();

        if (!cardinals.isEmpty()) {
            cardinals.forEach(Cardinal::done);
        }

        cardinal.inProgress();
    }
}
