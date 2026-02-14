package leets.weeth.domain.user.domain.repository;

import leets.weeth.domain.user.domain.entity.Cardinal;
import leets.weeth.domain.user.domain.entity.User;
import leets.weeth.domain.user.domain.entity.enums.Status;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByKakaoId(long kakaoId);

    Optional<User> findByAppleId(String appleId);

    List<User>findAllByNameContainingAndStatus(String name, Status status);

    boolean existsByEmail(String email);

    boolean existsByStudentId(String studentId);

    boolean existsByTel(String tel);

    boolean existsByStudentIdAndIdIsNot(String studentId, Long id);

    boolean existsByTelAndIdIsNot(String tel, Long id);

    List<User> findAllByStatusOrderByName(Status status);

    List<User> findAllByOrderByNameAsc();

    @Query("SELECT uc.user FROM UserCardinal uc WHERE uc.cardinal = :cardinal AND uc.user.status = :status")
    List<User> findAllByCardinalAndStatus(@Param("cardinal") Cardinal cardinal, @Param("status") Status status);

    /*
    todo 차후 리팩토링
     */
    @Query("""
                SELECT u
                FROM User u
                JOIN UserCardinal uc ON u.id = uc.user.id
                JOIN uc.cardinal c
                WHERE u.status = :status
                GROUP BY u.id
                ORDER BY MAX(c.cardinalNumber) DESC, u.name ASC
            """)
    Slice<User> findAllByStatusOrderedByCardinalAndName(@Param("status") Status status, Pageable pageable);

    @Query("""
                SELECT u FROM User u
                JOIN UserCardinal uc ON uc.user.id = u.id
                WHERE u.status = :status
                AND uc.cardinal = :cardinal
                ORDER BY u.name ASC
            """)
    Slice<User> findAllByCardinalOrderByNameAsc(@Param("status") Status status, @Param("cardinal") Cardinal cardinal, Pageable pageable);
}
