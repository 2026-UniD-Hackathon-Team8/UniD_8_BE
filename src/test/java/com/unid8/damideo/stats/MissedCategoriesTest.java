package com.unid8.damideo.stats;

import static org.assertj.core.api.Assertions.assertThat;

import com.unid8.damideo.stats.MissedCategories.CategoryAnswer;
import java.util.List;
import org.junit.jupiter.api.Test;

class MissedCategoriesTest {

    private static final List<String> ORDER = List.of("EVIDENCE", "CONTEXT", "LOGIC", "TIMELINE", "QUESTION");

    @Test
    void 가장_많이_틀린_계열을_돌려준다() {
        List<CategoryAnswer> answers = List.of(
                new CategoryAnswer("LOGIC", false),
                new CategoryAnswer("LOGIC", false),
                new CategoryAnswer("EVIDENCE", false),
                new CategoryAnswer("QUESTION", true));

        assertThat(MissedCategories.mostMissed(ORDER, answers)).containsExactly("LOGIC");
    }

    @Test
    void 동률이면_계열_순서대로_모두_돌려준다() {
        List<CategoryAnswer> answers = List.of(
                new CategoryAnswer("QUESTION", false),
                new CategoryAnswer("CONTEXT", false));

        assertThat(MissedCategories.mostMissed(ORDER, answers)).containsExactly("CONTEXT", "QUESTION");
    }

    @Test
    void 다_맞히면_빈_목록() {
        assertThat(MissedCategories.mostMissed(ORDER, List.of(new CategoryAnswer("LOGIC", true)))).isEmpty();
    }
}
