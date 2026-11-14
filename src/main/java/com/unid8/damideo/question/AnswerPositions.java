package com.unid8.damideo.question;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 한 세트 안에서 정답 위치(A/B)를 반씩 나눈 뒤 섞는다.
 * 문제마다 따로 정하면 한쪽으로 몰릴 수 있어서, 한쪽만 고르는 요령이 통하지 않게 한다.
 */
public final class AnswerPositions {

    private AnswerPositions() {
    }

    /**
     * @return 길이가 count인 위치 목록. 홀수면 남는 하나만 무작위
     */
    public static List<AnswerPos> balanced(int count, Random random) {
        List<AnswerPos> positions = new ArrayList<>(count);
        for (int i = 0; i < count / 2; i++) {
            positions.add(AnswerPos.A);
            positions.add(AnswerPos.B);
        }
        if (count % 2 == 1) {
            positions.add(random.nextBoolean() ? AnswerPos.A : AnswerPos.B);
        }
        Collections.shuffle(positions, random);
        return positions;
    }
}
