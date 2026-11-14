package com.unid8.damideo.daily;

import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailySetRepository extends JpaRepository<DailySet, Long> {

    Optional<DailySet> findByDailyDate(LocalDate dailyDate);
}
