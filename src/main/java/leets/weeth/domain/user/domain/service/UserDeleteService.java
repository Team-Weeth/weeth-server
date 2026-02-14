package leets.weeth.domain.user.domain.service;

import jakarta.transaction.Transactional;
import leets.weeth.domain.user.domain.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDeleteService {

    @Transactional
    public void leave(User user) {
        user.leave();
    }

    @Transactional
    public void ban(User user) {
        user.ban();
    }
}
