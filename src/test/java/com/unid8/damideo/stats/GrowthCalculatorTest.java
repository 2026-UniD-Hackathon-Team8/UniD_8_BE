package com.unid8.damideo.stats;

import static org.assertj.core.api.Assertions.assertThat;

import com.unid8.damideo.stats.GrowthCalculator.Growth;
import com.unid8.damideo.stats.GrowthCalculator.Round;
import java.util.List;
import org.junit.jupiter.api.Test;

class GrowthCalculatorTest {

    @Test
    void 처음_3회와_최근_3회를_문제_단위로_비교한다() {
        Growth growth = GrowthCalculator.calculate(List.of(
                new Round(5, 10, false),
                new Round(2, 5, false),
                new Round(6, 10, false),
                new Round(4, 5, false),
                new Round(8, 10, false),
                new Round(9, 10, false),
                new Round(5, 5, false)));

        // 처음 3회: (5+2+6)/(10+5+10) = 52%, 최근 3회: (8+9+5)/(10+10+5) = 88%
        assertThat(growth.firstScore()).isEqualTo(52);
        assertThat(growth.recentScore()).isEqualTo(88);
        assertThat(growth.change()).isEqualTo(36);
        assertThat(growth.scores()).containsExactly(50, 40, 60, 80, 80, 90, 100);
    }

    @Test
    void 약점_재도전_판은_뺀다() {
        Growth growth = GrowthCalculator.calculate(List.of(
                new Round(5, 10, false),
                new Round(0, 10, true),
                new Round(9, 10, false)));

        assertThat(growth.scores()).containsExactly(50, 90);
        assertThat(growth.change()).isEqualTo(40);
    }

    @Test
    void 회가_적으면_절반씩_나눠_비교한다() {
        Growth growth = GrowthCalculator.calculate(List.of(
                new Round(4, 10, false),
                new Round(6, 10, false),
                new Round(7, 10, false),
                new Round(9, 10, false)));

        // 4회면 처음 2회와 최근 2회: 50% vs 80%
        assertThat(growth.firstScore()).isEqualTo(50);
        assertThat(growth.recentScore()).isEqualTo(80);
    }

    @Test
    void 한_회뿐이면_비교하지_않는다() {
        Growth growth = GrowthCalculator.calculate(List.of(new Round(3, 5, false)));

        assertThat(growth.scores()).containsExactly(60);
        assertThat(growth.firstScore()).isNull();
        assertThat(growth.change()).isNull();
    }
}
