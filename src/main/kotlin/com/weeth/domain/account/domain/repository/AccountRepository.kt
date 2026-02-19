package com.weeth.domain.account.domain.repository;

import com.weeth.domain.account.domain.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByCardinal(Integer cardinal);

    boolean existsByCardinal(Integer cardinal);
}
