package com.unid8.damideo.question;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class AnswerPositionsTest {

    // 짝수면 A와 B가 정확히 반씩이다
    @Test
    void balanced_evenCountSplitsInHalf() {
        for (long seed = 0; seed < 50; seed++) {
            List<AnswerPos> positions = AnswerPositions.balanced(10, new Random(seed));

            assertThat(positions).hasSize(10);
            assertThat(Collections.frequency(positions, AnswerPos.A)).isEqualTo(5);
        }
    }

    // 홀수면 한쪽이 하나만 더 많다
    @Test
    void balanced_oddCountDiffersByOne() {
        for (long seed = 0; seed < 50; seed++) {
            List<AnswerPos> positions = AnswerPositions.balanced(5, new Random(seed));

            int a = Collections.frequency(positions, AnswerPos.A);
            assertThat(positions).hasSize(5);
            assertThat(a).isBetween(2, 3);
        }
    }

    // 같은 시드면 같은 순서다 (데일리는 날짜 시드로 모두가 같은 세트를 본다)
    @Test
    void balanced_sameSeedSameOrder() {
        assertThat(AnswerPositions.balanced(5, new Random(42)))
                .isEqualTo(AnswerPositions.balanced(5, new Random(42)));
    }
}
