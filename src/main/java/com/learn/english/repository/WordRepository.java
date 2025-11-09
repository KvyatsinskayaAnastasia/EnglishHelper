package com.learn.english.repository;

import com.learn.english.entity.WordEO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface WordRepository extends JpaRepository<WordEO, UUID>, PagingAndSortingRepository<WordEO, UUID> {

    int countAllByRepeatAtIsLessThanEqualAndUserId(LocalDateTime repeatAt, Long userId);

    Page<WordEO> findAllByRepeatAtIsLessThanEqualAndUserId(LocalDateTime repeatAt, Long userId, Pageable pageable);

    @Query(value = """
            SELECT DISTINCT w.userId as userId FROM WordEO w
                        WHERE w.repeatAt <= :currentTime
            """)
    List<Long> findDistinctUsersWithWordsToRepeat(@Param("currentTime") LocalDateTime repeatAt);

    @Query(value = """
            SELECT * FROM word
            WHERE user_id = :userId
            AND (:excludeWordId IS NULL OR id != :excludeWordId)
            ORDER BY RANDOM()
            LIMIT :size
            """, nativeQuery = true)
    List<WordEO> getRandomWordEOSByUserIdExcludeWordWithId(
            @Param("userId") Long userId,
            @Param("excludeWordId") UUID excludeWordId,
            @Param("size") Integer size
    );

    List<WordEO> findWordEOSByRepeatedCountGreaterThanEqual(int repeatedCount);
}
