package leets.weeth.domain.user.domain.repository;

import java.util.List;
import leets.weeth.domain.user.domain.entity.User;
import leets.weeth.domain.user.domain.entity.UserCardinal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserCardinalRepository extends JpaRepository<UserCardinal, Long> {

    List<UserCardinal> findAllByUserOrderByCardinalCardinalNumberDesc(User user);

    @Query("SELECT uc FROM UserCardinal uc WHERE uc.user IN :users ORDER BY uc.user.id, uc.cardinal.cardinalNumber DESC")
    List<UserCardinal> findAllByUsers(List<User> users);

	List<UserCardinal> findAllByOrderByUser_NameAsc();

    @Query("""
        select uc.cardinal.cardinalNumber
          from UserCardinal uc
         where uc.user = :user
      order by uc.cardinal.cardinalNumber desc
    """)
    List<Integer> findCardinalNumbersByUser(@Param("user") User user);
}
