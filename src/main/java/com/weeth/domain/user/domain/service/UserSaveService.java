package com.weeth.domain.user.domain.service;

import com.weeth.domain.user.domain.entity.User;
import com.weeth.domain.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserSaveService {

    private final UserRepository userRepository;

    public void save(User user) {
        userRepository.save(user);
    }
}
