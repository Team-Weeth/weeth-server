package com.weeth.domain.board.domain.repository;

import com.weeth.domain.board.domain.entity.Notice;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "2000"))
    @Query("select n from Notice n where n.id = :id")
    Notice findByIdWithLock(@Param("id") Long id);

    Slice<Notice> findPageBy(Pageable page);

    @Query("""
        SELECT n FROM Notice n
        WHERE (LOWER(n.title)   LIKE LOWER(CONCAT('%', :kw, '%'))
            OR LOWER(n.content) LIKE LOWER(CONCAT('%', :kw, '%')))
        ORDER BY n.id DESC
    """)
    Slice<Notice> search(@Param("kw") String kw, Pageable pageable);
}
