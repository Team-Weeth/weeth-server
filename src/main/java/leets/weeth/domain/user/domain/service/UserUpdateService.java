package leets.weeth.domain.user.domain.service;

import jakarta.transaction.Transactional;
import leets.weeth.domain.user.domain.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import static leets.weeth.domain.user.application.dto.request.UserRequestDto.Update;


@Service
@Transactional
@RequiredArgsConstructor
public class UserUpdateService {

    public void update(User user, Update dto) {
        user.update(dto);
    }

    public void accept(User user) {
        user.accept();
    }

    public void update(User user, String role) {
        user.update(role);
    }

    public void reset(User user, PasswordEncoder passwordEncoder) {
        user.reset(passwordEncoder);
    }
}
