package com.unid8.damideo.stats;

import static org.assertj.core.api.Assertions.assertThat;

import com.unid8.damideo.stats.WeaknessTypeJudge.TypeJudgement;
import com.unid8.damideo.stats.WeaknessTypeJudge.TypeState;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class WeaknessTypeJudgeTest {

    private static final List<String> ORDER = List.of("EVIDENCE", "CONTEXT", "LOGIC", "TIMELINE", "QUESTION");
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 15);

    @Test
    void 한_계열이라도_3문제면_유형이_열리지_않는다() {
        Map<String, List<Boolean>> answers = allSame(6, 4);
        answers.put("LOGIC", answers(3, 1));

        TypeJudgement result = judge(answers, TypeState.empty());

        assertThat(result.opened()).isFalse();
        assertThat(result.currentType()).isNull();
    }

    @Test
    void 모든_계열이_4문제면_유형이_열린다() {
        Map<String, List<Boolean>> answers = allSame(4, 3);
        answers.put("TIMELINE", answers(4, 1));

        TypeJudgement result = judge(answers, TypeState.empty());

        assertThat(result.opened()).isTrue();
        assertThat(result.changed()).isTrue();
        assertThat(result.currentType()).isEqualTo("TIMELINE");
    }

    @Test
    void 첫_판정에서_동률이면_계열_순서가_앞선_쪽() {
        Map<String, List<Boolean>> answers = allSame(6, 5);
        answers.put("CONTEXT", answers(6, 2));
        answers.put("QUESTION", answers(6, 2));

        assertThat(judge(answers, TypeState.empty()).currentType()).isEqualTo("CONTEXT");
    }

    @Test
    void 모든_계열이_80퍼센트_이상이면_탐정형() {
        Map<String, List<Boolean>> answers = allSame(6, 5);
        answers.put("LOGIC", answers(5, 4));

        assertThat(judge(answers, TypeState.empty()).currentType()).isEqualTo(WeaknessTypeJudge.DETECTIVE);
    }

    @Test
    void 최근_6문제만_본다() {
        Map<String, List<Boolean>> answers = allSame(6, 4);
        List<Boolean> evidence = new ArrayList<>(answers(6, 6));
        evidence.addAll(answers(6, 0));
        answers.put("EVIDENCE", evidence);

        TypeJudgement result = judge(answers, TypeState.empty());

        assertThat(result.accuracies().get("EVIDENCE")).isEqualTo(1.0);
        assertThat(result.currentType()).isNotEqualTo("EVIDENCE");
    }

    @Test
    void 지금_유형보다_10퍼센트포인트_낮은_계열은_후보가_된다() {
        // EVIDENCE 3/5(60%), LOGIC 3/6(50%): 차이 10%p
        Map<String, List<Boolean>> answers = allSame(6, 5);
        answers.put("EVIDENCE", answers(5, 3));
        answers.put("LOGIC", answers(6, 3));
        TypeState state = new TypeState("EVIDENCE", null, null);

        TypeJudgement result = judge(answers, state);

        assertThat(result.changed()).isFalse();
        assertThat(result.currentType()).isEqualTo("EVIDENCE");
        assertThat(result.candidateType()).isEqualTo("LOGIC");
        assertThat(result.candidateSince()).isEqualTo(TODAY);
    }

    @Test
    void 차이가_10퍼센트포인트보다_작으면_유지한다() {
        // EVIDENCE 4/6(67%), LOGIC 3/5(60%): 차이 7%p
        Map<String, List<Boolean>> answers = allSame(6, 5);
        answers.put("EVIDENCE", answers(6, 4));
        answers.put("LOGIC", answers(5, 3));
        TypeState state = new TypeState("EVIDENCE", "LOGIC", TODAY.minusDays(1));

        TypeJudgement result = judge(answers, state);

        assertThat(result.currentType()).isEqualTo("EVIDENCE");
        assertThat(result.candidateType()).isNull();
    }

    @Test
    void 같은_날에는_후보로만_남는다() {
        Map<String, List<Boolean>> answers = weakLogic();
        TypeState state = new TypeState("EVIDENCE", "LOGIC", TODAY);

        TypeJudgement result = judge(answers, state);

        assertThat(result.changed()).isFalse();
        assertThat(result.currentType()).isEqualTo("EVIDENCE");
        assertThat(result.candidateSince()).isEqualTo(TODAY);
    }

    @Test
    void 다음_날에도_같은_후보면_유형이_바뀐다() {
        Map<String, List<Boolean>> answers = weakLogic();
        TypeState state = new TypeState("EVIDENCE", "LOGIC", TODAY.minusDays(1));

        TypeJudgement result = judge(answers, state);

        assertThat(result.changed()).isTrue();
        assertThat(result.currentType()).isEqualTo("LOGIC");
        assertThat(result.candidateType()).isNull();
    }

    @Test
    void 후보가_달라지면_오늘부터_다시_센다() {
        Map<String, List<Boolean>> answers = weakLogic();
        TypeState state = new TypeState("EVIDENCE", "TIMELINE", TODAY.minusDays(1));

        TypeJudgement result = judge(answers, state);

        assertThat(result.changed()).isFalse();
        assertThat(result.candidateType()).isEqualTo("LOGIC");
        assertThat(result.candidateSince()).isEqualTo(TODAY);
    }

    @Test
    void 지금_유형이_가장_낮으면_후보를_지운다() {
        Map<String, List<Boolean>> answers = allSame(6, 5);
        answers.put("EVIDENCE", answers(6, 2));
        TypeState state = new TypeState("EVIDENCE", "LOGIC", TODAY.minusDays(1));

        TypeJudgement result = judge(answers, state);

        assertThat(result.currentType()).isEqualTo("EVIDENCE");
        assertThat(result.candidateType()).isNull();
        assertThat(result.candidateSince()).isNull();
    }

    @Test
    void 탐정형에서_80퍼센트_밑으로_떨어진_계열이_생기면_후보가_된다() {
        Map<String, List<Boolean>> answers = allSame(6, 6);
        answers.put("QUESTION", answers(6, 4));
        TypeState state = new TypeState(WeaknessTypeJudge.DETECTIVE, null, null);

        assertThat(judge(answers, state).candidateType()).isEqualTo("QUESTION");
    }

    private static TypeJudgement judge(Map<String, List<Boolean>> answers, TypeState state) {
        return WeaknessTypeJudge.judge(ORDER, answers, state, TODAY);
    }

    /** EVIDENCE 3/6(50%), LOGIC 2/6(33%), 나머지 5/6 */
    private static Map<String, List<Boolean>> weakLogic() {
        Map<String, List<Boolean>> answers = allSame(6, 5);
        answers.put("EVIDENCE", answers(6, 3));
        answers.put("LOGIC", answers(6, 2));
        return answers;
    }

    private static Map<String, List<Boolean>> allSame(int size, int correct) {
        Map<String, List<Boolean>> result = new HashMap<>();
        for (String category : ORDER) {
            result.put(category, answers(size, correct));
        }
        return result;
    }

    /** 최신순 size개 중 correct개가 정답 */
    private static List<Boolean> answers(int size, int correct) {
        List<Boolean> result = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            result.add(i < correct);
        }
        return result;
    }
}
