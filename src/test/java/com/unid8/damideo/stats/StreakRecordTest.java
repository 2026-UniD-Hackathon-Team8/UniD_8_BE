package com.unid8.damideo.stats;

import static org.assertj.core.api.Assertions.assertThat;

import com.unid8.damideo.stats.StreakRecord.Best;
import com.unid8.damideo.stats.StreakRecord.Entry;
import com.unid8.damideo.stats.StreakRecord.Ranked;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class StreakRecordTest {

    private static final LocalDateTime T1 = LocalDateTime.of(2026, 11, 15, 1, 0);
    private static final LocalDateTime T2 = T1.plusHours(1);
    private static final LocalDateTime T3 = T1.plusHours(2);

    @Test
    void 최고_기록보다_높으면_갱신한다() {
        assertThat(StreakRecord.updateBest(new Best(3, T1), 5, T2)).isEqualTo(new Best(5, T2));
        assertThat(StreakRecord.updateBest(Best.none(), 1, T1)).isEqualTo(new Best(1, T1));
    }

    @Test
    void 같거나_낮으면_먼저_달성한_기록을_유지한다() {
        assertThat(StreakRecord.updateBest(new Best(5, T1), 5, T2)).isEqualTo(new Best(5, T1));
        assertThat(StreakRecord.updateBest(new Best(5, T1), 2, T2)).isEqualTo(new Best(5, T1));
    }

    @Test
    void 연승_내림차순_같으면_먼저_달성한_순이고_동점은_같은_순위() {
        List<Ranked<String>> ranked = StreakRecord.rank(List.of(
                new Entry<>("c", new Best(3, T1)),
                new Entry<>("b", new Best(7, T2)),
                new Entry<>("a", new Best(7, T1)),
                new Entry<>("d", new Best(5, T3))));

        assertThat(ranked).extracting(Ranked::owner).containsExactly("a", "b", "d", "c");
        assertThat(ranked).extracting(Ranked::rank).containsExactly(1, 1, 3, 4);
    }
}
