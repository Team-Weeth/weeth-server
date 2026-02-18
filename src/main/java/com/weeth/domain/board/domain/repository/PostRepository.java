package com.weeth.domain.board.domain.repository;

import com.weeth.domain.board.domain.entity.Post;
import com.weeth.domain.board.domain.entity.enums.Category;
import com.weeth.domain.board.domain.entity.enums.Part;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "2000"))
	@Query("select p from Post p where p.id = :id")
	Post findByIdWithLock(@Param("id") Long id);

	@Query("""
        SELECT p FROM Post p
        WHERE p.category IN (
            com.weeth.domain.board.domain.entity.enums.Category.StudyLog,
            com.weeth.domain.board.domain.entity.enums.Category.Article
        )
        ORDER BY p.id DESC
    """)
	Slice<Post> findRecentPart(Pageable pageable);

	@Query("""
        SELECT p FROM Post p
        WHERE p.category = com.weeth.domain.board.domain.entity.enums.Category.Education
        ORDER BY p.id DESC
    """)
	Slice<Post> findRecentEducation(Pageable pageable);

	@Query("""
        SELECT p FROM Post p
        WHERE p.category IN (
            com.weeth.domain.board.domain.entity.enums.Category.StudyLog,
            com.weeth.domain.board.domain.entity.enums.Category.Article
        )
          AND (
                LOWER(p.title)   LIKE LOWER(CONCAT('%', :kw, '%'))
             OR LOWER(p.content) LIKE LOWER(CONCAT('%', :kw, '%'))
          )
        ORDER BY p.id DESC
    """)
	Slice<Post> searchPart(@Param("kw") String kw, Pageable pageable);

	@Query("""
        SELECT p FROM Post p
        WHERE p.category = com.weeth.domain.board.domain.entity.enums.Category.Education
          AND (
                LOWER(p.title)   LIKE LOWER(CONCAT('%', :kw, '%'))
             OR LOWER(p.content) LIKE LOWER(CONCAT('%', :kw, '%'))
          )
        ORDER BY p.id DESC
    """)
	Slice<Post> searchEducation(@Param("kw") String kw, Pageable pageable);

	@Query("""
		SELECT DISTINCT p.studyName
		FROM Post p
		WHERE (:part = com.weeth.domain.board.domain.entity.enums.Part.ALL OR p.part = :part)
		  AND p.studyName IS NOT NULL
		ORDER BY p.studyName ASC
	""")
	List<String> findDistinctStudyNamesByPart(@Param("part") Part part);

	@Query("""
        SELECT p
          FROM Post p
         WHERE (p.part = :part OR p.part = com.weeth.domain.board.domain.entity.enums.Part.ALL OR :part = com.weeth.domain.board.domain.entity.enums.Part.ALL
         )
           AND (:category IS NULL OR p.category = :category)
           AND (:cardinal IS NULL OR p.cardinalNumber = :cardinal)
           AND (:studyName IS NULL OR p.studyName = :studyName)
           AND (:week IS NULL OR p.week = :week)
      ORDER BY p.id DESC
    """)
	Slice<Post> findByPartAndOptionalFilters(@Param("part") Part part, @Param("category") Category category, @Param("cardinal") Integer cardinal, @Param("studyName") String studyName, @Param("week") Integer week, Pageable pageable);

	@Query("""
		SELECT p
		  FROM Post p
		 WHERE p.category = :category
		   AND (:cardinal IS NULL OR p.cardinalNumber = :cardinal)
		   AND (
				 :partName = 'ALL'
			  OR FUNCTION('FIND_IN_SET', :partName, p.parts) > 0
			  OR FUNCTION('FIND_IN_SET', 'ALL',    p.parts) > 0
			   )
	  ORDER BY p.id DESC
	""")
	Slice<Post> findByCategoryAndOptionalCardinalWithPart(@Param("partName") String partName, @Param("category") Category category, @Param("cardinal") Integer cardinal, Pageable pageable);

	@Query("""
		SELECT p
		  FROM Post p
		 WHERE p.category = :category
		   AND p.cardinalNumber = :cardinal
		   AND (
				 :partName = 'ALL'
			  OR FUNCTION('FIND_IN_SET', :partName, p.parts) > 0
			  OR FUNCTION('FIND_IN_SET', 'ALL',    p.parts) > 0
			 )
	  ORDER BY p.id DESC
	""")
	Slice<Post> findByCategoryAndCardinalNumberWithPart(@Param("partName") String partName, @Param("category") Category category, @Param("cardinal") Integer cardinal, Pageable pageable);

	@Query("""
		SELECT p
		  FROM Post p
		 WHERE p.category = :category
		   AND p.cardinalNumber IN :cardinals
		   AND (
				 :partName = 'ALL'
			  OR FUNCTION('FIND_IN_SET', :partName, p.parts) > 0
			  OR FUNCTION('FIND_IN_SET', 'ALL',    p.parts) > 0
			 )
	  ORDER BY p.id DESC
	""")
	Slice<Post> findByCategoryAndCardinalInWithPart(@Param("partName") String partName, @Param("category") Category category, @Param("cardinals") Collection<Integer> cardinals, Pageable pageable);
}
