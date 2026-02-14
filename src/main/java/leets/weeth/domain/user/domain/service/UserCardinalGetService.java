package leets.weeth.domain.user.domain.service;

import java.util.Comparator;
import java.util.List;
import leets.weeth.domain.user.application.exception.CardinalNotFoundException;
import leets.weeth.domain.user.domain.entity.Cardinal;
import leets.weeth.domain.user.domain.entity.User;
import leets.weeth.domain.user.domain.entity.UserCardinal;
import leets.weeth.domain.user.domain.repository.UserCardinalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserCardinalGetService {

    private final UserCardinalRepository userCardinalRepository;

    public List<UserCardinal> getUserCardinals(User user) {
        return userCardinalRepository.findAllByUserOrderByCardinalCardinalNumberDesc(user);
    }

    public List<UserCardinal> findAll() {
        return userCardinalRepository.findAllByOrderByUser_NameAsc();
    }

    public List<UserCardinal> findAll(List<User> users) {
        return userCardinalRepository.findAllByUsers(users);
    }

    public boolean notContains(User user, Cardinal cardinal) {
        return getUserCardinals(user).stream()
                .noneMatch(userCardinal -> userCardinal.getCardinal().equals(cardinal));
    }

    public boolean isCurrent(User user, Cardinal cardinal) {
        Integer maxCardinalNumber = getUserCardinals(user).stream()
                .map(UserCardinal::getCardinal)
                .map(Cardinal::getCardinalNumber)
                .max(Integer::compareTo)
                .orElseThrow(CardinalNotFoundException::new);

        return maxCardinalNumber < cardinal.getCardinalNumber();
    }

    public Cardinal getCurrentCardinal(User user) {
        return getUserCardinals(user).stream()
                .map(UserCardinal::getCardinal)
                .max(Comparator.comparing(Cardinal::getCardinalNumber))
                .orElseThrow(CardinalNotFoundException::new);
    }

    public List<Integer> getCardinalNumbers(User user) {
        return userCardinalRepository.findCardinalNumbersByUser(user);
    }
}
