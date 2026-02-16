package com.weeth.domain.user.domain.service;

import com.weeth.domain.user.domain.entity.Cardinal;
import com.weeth.domain.user.domain.repository.CardinalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CardinalSaveService {

    private final CardinalRepository cardinalRepository;

    public Cardinal save(Cardinal cardinal) {
        return cardinalRepository.save(cardinal);
    }
}
