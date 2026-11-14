package com.unid8.damideo.game;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GameSessionQuestionRepository extends JpaRepository<GameSessionQuestion, Long> {

    @Query("""
            SELECT gsq
            FROM GameSessionQuestion gsq
            JOIN FETCH gsq.question
            WHERE gsq.gameSession.id = :gameSessionId
            ORDER BY gsq.seq ASC
            """)
    List<GameSessionQuestion> findAllWithQuestionByGameSessionId(@Param("gameSessionId") Long gameSessionId);
}
