package leets.weeth.domain.user.domain.service;

import leets.weeth.domain.user.domain.entity.Cardinal;
import leets.weeth.domain.user.domain.entity.User;
import leets.weeth.domain.user.domain.entity.enums.Status;
import leets.weeth.domain.user.domain.repository.UserRepository;
import leets.weeth.domain.user.application.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserGetService {

    private final UserRepository userRepository;

    public User find(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
    }

    public User find(String email){
        return userRepository.findByEmail(email)
                .orElseThrow(UserNotFoundException::new);
    }

    public Optional<User> find(long kakaoId){
        return userRepository.findByKakaoId(kakaoId);
    }

    public Optional<User> findByAppleId(String appleId){
        return userRepository.findByAppleId(appleId);
    }

    public List<User> search(String keyword) {
        return userRepository.findAllByNameContainingAndStatus(keyword, Status.ACTIVE);
    }

    public Boolean check(String email) {
        return !userRepository.existsByEmail(email);
    }

    public List<User> findAll(List<Long> userId) {
        return userRepository.findAllById(userId);
    }

    public List<User> findAllByCardinal(Cardinal cardinal) {
        return userRepository.findAllByCardinalAndStatus(cardinal, Status.ACTIVE);
    }

    public Slice<User> findAll(Pageable pageable) {
        Slice<User> users = userRepository.findAllByStatusOrderedByCardinalAndName(Status.ACTIVE, pageable);

        if (users.isEmpty()) {
            throw new UserNotFoundException();
        }

        return users;
    }

    public Slice<User> findAll(Pageable pageable, Cardinal cardinal) {
        Slice<User> users = userRepository.findAllByCardinalOrderByNameAsc(Status.ACTIVE, cardinal, pageable);

        if (users.isEmpty()) {
            throw new UserNotFoundException();
        }

        return users;
    }

    public boolean validateStudentId(String studentId) {
        return userRepository.existsByStudentId(studentId);
    }

    public boolean validateStudentId(String studentId, Long userId) {
        return userRepository.existsByStudentIdAndIdIsNot(studentId, userId);
    }

    public boolean validateTel(String tel) {
        return userRepository.existsByTel(tel);
    }

    public boolean validateTel(String tel, Long userId) {
        return userRepository.existsByTelAndIdIsNot(tel, userId);
    }
}
