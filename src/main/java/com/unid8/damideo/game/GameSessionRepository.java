package com.unid8.damideo.game;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameSessionRepository extends JpaRepository<GameSession, Long> {

    List<GameSession> findAllByUserIdAndStatus(Long userId, GameStatus status);

    Optional<GameSession> findFirstByUserIdAndStatusOrderByIdDesc(Long userId, GameStatus status);
}
