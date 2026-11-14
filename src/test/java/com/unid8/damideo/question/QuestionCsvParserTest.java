package com.unid8.damideo.question;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.unid8.damideo.question.QuestionCsvParser.ParseResult;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class QuestionCsvParserTest {

    private final QuestionCsvParser parser = new QuestionCsvParser();

    @Test
    void parsesProblemsetCsvWithExactTypeRules() throws Exception {
        ClassPathResource resource = new ClassPathResource("data/problemset.csv");
        ParseResult result;
        try (InputStream is = resource.getInputStream()) {
            result = parser.parse(is);
        }

        assertThat(result.skippedRows()).isEmpty();
        assertThat(result.questions()).hasSizeGreaterThanOrEqualTo(10);

        List<String> notionIds = result.questions().stream()
                .map(Question::getNotionId)
                .toList();
        assertThat(notionIds).doesNotHaveDuplicates();

        Map<QuestionType, Long> countsByType = result.questions().stream()
                .collect(Collectors.groupingBy(Question::getQuestionType, Collectors.counting()));
        assertThat(countsByType)
                .containsKeys(
                        QuestionType.CHOICE_ANSWER,
                        QuestionType.CHOICE_QUESTION,
                        QuestionType.CHOICE_DIALOGUE
                );

        long activeDailyCount = result.questions().stream()
                .filter(q -> q.isActive() && q.isDailyEligible())
                .count();
        assertThat(activeDailyCount).isGreaterThanOrEqualTo(5);

        for (Question question : result.questions()) {
            assertThat(question.getNotionId()).startsWith("DAMIT-");
            assertThat(question.getCategory()).isNotNull();
            assertThat(QuestionTactic.valueOf(question.getTactic())).isNotNull();
            assertThat(QuestionTacticNames.resolve(question.getTactic()))
                    .isNotBlank()
                    .isNotEqualTo(question.getTactic());
            assertThat(question.getDifficulty()).isBetween((byte) 1, (byte) 3);

            if (question.getQuestionType() == QuestionType.CHOICE_QUESTION) {
                assertThat(question.getCredibleAiAnswer()).isNotBlank();
                assertThat(question.getTargetAiAnswer()).isNotBlank();
            } else {
                assertThat(question.getCredibleAiAnswer()).isNull();
                assertThat(question.getTargetAiAnswer()).isNull();
            }
        }

        assertThat(result.questions())
                .filteredOn(q -> q.getQuestionType() == QuestionType.CHOICE_DIALOGUE)
                .isNotEmpty()
                .anySatisfy(q -> {
                    assertThat(q.getPrompt()).contains("\nAI: ");
                    assertThat(q.getPrompt().lines().count()).isGreaterThanOrEqualTo(2);
                });
    }

    @Test
    void skipsRowsWithMissingRequiredColumnsAndMapsInactiveStatus() throws Exception {
        String csv = "\uFEFF" + """
                문제,ID,골라낼 쪽 AI 답,골라낼 쪽(정답),계열,난이도,데일리용,상태,수법,유형,작성자,정답률(사전 풀이),믿을 쪽 AI 답,믿을 쪽,질문·상황,출처,해설,확인 질문
                제목1,DAMIT-101,,오답 보기,근거,쉬움,True,초안,1 가짜 출처,답 고르기,테스터,,,믿을 보기,질문입니다,https://example.com,해설입니다,확인 질문?
                제목2,DAMIT-102,,오답 질문,질문,보통,True,확정,15 맞장구,질문 고르기,테스터,,믿을 AI답,믿을 질문,상황입니다,https://example.com,해설입니다,확인 질문?
                제목3,DAMIT-103,,오답 보기,근거,,True,확정,1 가짜 출처,답 고르기,테스터,,,믿을 보기,질문입니다,https://example.com,해설입니다,확인 질문?
                """;

        ParseResult result = parser.parse(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

        assertThat(result.questions()).hasSize(1);
        Question activeFalseQuestion = result.questions().get(0);
        assertThat(activeFalseQuestion.getNotionId()).isEqualTo("DAMIT-101");
        assertThat(activeFalseQuestion.isActive()).isFalse();

        assertThat(result.skippedRows()).hasSize(2);
        assertThat(result.skippedRows().get(0).notionId()).isEqualTo("DAMIT-102");
        assertThat(result.skippedRows().get(0).missingColumns()).containsExactly("골라낼 쪽 AI 답");
        assertThat(result.skippedRows().get(1).notionId()).isEqualTo("DAMIT-103");
        assertThat(result.skippedRows().get(1).missingColumns()).containsExactly("난이도");
    }

    @Test
    void validatesTacticNumberAndNameMismatch() {
        assertThat(QuestionTactic.fromCsvValue("15 맞장구")).isEqualTo(QuestionTactic.SYCOPHANCY);
        assertThat(QuestionTactic.fromCsvValue("12 계산·단위 착오")).isEqualTo(QuestionTactic.CALC_UNIT_ERROR);
        assertThatThrownBy(() -> QuestionTactic.fromCsvValue("15 가짜 출처"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> QuestionTactic.fromCsvValue("99 없는수법"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
