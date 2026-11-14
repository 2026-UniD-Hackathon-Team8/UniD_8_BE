package com.unid8.damideo.streak;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StreakChallengeRepository extends JpaRepository<StreakChallenge, Long> {

    List<StreakChallenge> findAllByUserIdAndStatus(Long userId, StreakStatus status);
}
