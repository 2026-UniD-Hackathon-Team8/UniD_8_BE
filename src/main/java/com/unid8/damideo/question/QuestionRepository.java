package com.unid8.damideo.question;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findAllByActiveTrue();

    List<Question> findAllByActiveTrueAndDailyEligibleTrue();

    List<Question> findAllByNotionIdAndActiveTrue(String notionId);

    Optional<Question> findTopByNotionIdOrderByIdDesc(String notionId);
}
