package com.unid8.damideo.stats;

import static org.assertj.core.api.Assertions.assertThat;

import com.unid8.damideo.stats.DailyStreak.Record;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class DailyStreakTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 15);

    @Test
    void 어제_완료했으면_기록을_그대로_보여준다() {
        assertThat(DailyStreak.effective(3, TODAY.minusDays(1), TODAY)).isEqualTo(3);
    }

    @Test
    void 이틀_전이_마지막이면_0으로_보여준다() {
        assertThat(DailyStreak.effective(3, TODAY.minusDays(2), TODAY)).isZero();
        assertThat(DailyStreak.effective(0, null, TODAY)).isZero();
    }

    @Test
    void 어제에_이어서_완료하면_1_늘어난다() {
        Record record = DailyStreak.complete(3, 5, TODAY.minusDays(1), TODAY);

        assertThat(record.currentStreak()).isEqualTo(4);
        assertThat(record.longestStreak()).isEqualTo(5);
        assertThat(record.lastCompletedDate()).isEqualTo(TODAY);
    }

    @Test
    void 끊겼다가_완료하면_1부터_다시_시작한다() {
        assertThat(DailyStreak.complete(3, 5, TODAY.minusDays(2), TODAY).currentStreak()).isEqualTo(1);
        assertThat(DailyStreak.complete(0, 0, null, TODAY).currentStreak()).isEqualTo(1);
    }

    @Test
    void 최장_기록을_넘으면_함께_갱신한다() {
        assertThat(DailyStreak.complete(5, 5, TODAY.minusDays(1), TODAY).longestStreak()).isEqualTo(6);
    }

    @Test
    void 같은_날_두_번_완료해도_한_번만_센다() {
        Record record = DailyStreak.complete(4, 5, TODAY, TODAY);

        assertThat(record.currentStreak()).isEqualTo(4);
    }
}
