package com.weeth.domain.user.domain.service;

import com.weeth.domain.user.domain.entity.UserCardinal;
import com.weeth.domain.user.domain.repository.UserCardinalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserCardinalSaveService {

    private final UserCardinalRepository userCardinalRepository;

    public void save(UserCardinal userCardinal) {
        userCardinalRepository.save(userCardinal);
    }
}
