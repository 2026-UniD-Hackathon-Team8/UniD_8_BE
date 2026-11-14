package com.unid8.damideo.daily;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DailySetQuestionRepository extends JpaRepository<DailySetQuestion, Long> {

    @Query("""
            SELECT dsq
            FROM DailySetQuestion dsq
            JOIN FETCH dsq.question
            WHERE dsq.dailySet.id = :dailySetId
            ORDER BY dsq.seq ASC
            """)
    List<DailySetQuestion> findAllWithQuestionByDailySetId(@Param("dailySetId") Long dailySetId);

    @Query("""
            SELECT dsq.question.id
            FROM DailySetQuestion dsq
            WHERE dsq.dailySet.dailyDate >= :sinceDate
              AND dsq.dailySet.dailyDate < :beforeDate
            """)
    List<Long> findQuestionIdsBetweenDates(@Param("sinceDate") LocalDate sinceDate,
                                           @Param("beforeDate") LocalDate beforeDate);

    @Query("""
            SELECT dsq.question.id
            FROM DailySetQuestion dsq
            WHERE dsq.dailySet.dailyDate = :dailyDate
            """)
    List<Long> findQuestionIdsByDailyDate(@Param("dailyDate") LocalDate dailyDate);
}
