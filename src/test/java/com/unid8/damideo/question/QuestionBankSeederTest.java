package com.unid8.damideo.question;

import static org.assertj.core.api.Assertions.assertThat;

import com.unid8.damideo.TestcontainersConfiguration;
import com.unid8.damideo.common.time.KstDateTime;
import com.unid8.damideo.game.GameMode;
import com.unid8.damideo.question.QuestionBankSeeder.SeedSummary;
import com.unid8.damideo.question.QuestionCsvParser.ParseResult;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class QuestionBankSeederTest {

    @Autowired
    QuestionBankSeeder seeder;

    @Autowired
    QuestionSeedRunner seedRunner;

    @Autowired
    QuestionRepository questionRepository;

    @Autowired
    QuestionPicker questionPicker;

    @Autowired
    QuestionBankProperties properties;

    @Autowired
    ResourceLoader resourceLoader;

    @Autowired
    QuestionCsvParser questionCsvParser;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
        jdbcTemplate.execute("TRUNCATE TABLE answers");
        jdbcTemplate.execute("TRUNCATE TABLE wrong_answer_notes");
        jdbcTemplate.execute("TRUNCATE TABLE game_session_questions");
        jdbcTemplate.execute("TRUNCATE TABLE game_sessions");
        jdbcTemplate.execute("TRUNCATE TABLE daily_set_questions");
        jdbcTemplate.execute("TRUNCATE TABLE daily_sets");
        jdbcTemplate.execute("TRUNCATE TABLE streak_challenges");
        jdbcTemplate.execute("TRUNCATE TABLE questions");
        jdbcTemplate.execute("TRUNCATE TABLE users");
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
    }

    @Test
    void seedsConfiguredCsvIdempotentlyAndVerifiesAllThreeQuestionTypes() throws Exception {
        Resource resource = configuredResource();
        ParseResult parsed = parseConfiguredCsv(resource);
        int totalParsed = parsed.questions().size();
        int expectedActiveCount = (int) parsed.questions().stream().filter(Question::isActive).count();
        assertThat(totalParsed).isGreaterThanOrEqualTo(10);
        assertThat(parsed.skippedRows()).isEmpty();

        SeedSummary firstRun = seeder.seed(resource);
        assertThat(firstRun.totalParsed()).isEqualTo(totalParsed);
        assertThat(firstRun.inserted()).isEqualTo(totalParsed);
        assertThat(firstRun.updatedVersions()).isZero();
        assertThat(firstRun.skipped()).isZero();

        SeedSummary secondRun = seeder.seed(resource);
        assertThat(secondRun.totalParsed()).isEqualTo(totalParsed);
        assertThat(secondRun.inserted()).isZero();
        assertThat(secondRun.updatedVersions()).isZero();
        assertThat(secondRun.unchanged()).isEqualTo(totalParsed);

        List<Question> activeQuestions = questionRepository.findAllByActiveTrue();
        assertThat(activeQuestions).hasSize(expectedActiveCount);

        Map<QuestionType, List<Question>> byType = activeQuestions.stream()
                .collect(Collectors.groupingBy(Question::getQuestionType));
        assertThat(byType)
                .containsKeys(
                        QuestionType.CHOICE_ANSWER,
                        QuestionType.CHOICE_QUESTION,
                        QuestionType.CHOICE_DIALOGUE
                );
        assertThat(byType.get(QuestionType.CHOICE_ANSWER)).isNotEmpty();
        assertThat(byType.get(QuestionType.CHOICE_QUESTION)).isNotEmpty();
        assertThat(byType.get(QuestionType.CHOICE_DIALOGUE)).isNotEmpty();

        // 답 고르기 검증 (DAMIT-6)
        Question choiceAnswer = findByNotionId(activeQuestions, "DAMIT-6");
        assertThat(choiceAnswer.getCategory()).isEqualTo(QuestionCategory.EVIDENCE);
        assertThat(choiceAnswer.getTactic()).isEqualTo("FAKE_SOURCE");
        assertThat(choiceAnswer.getDifficulty()).isEqualTo((byte) 2);
        assertThat(choiceAnswer.getCredibleAiAnswer()).isNull();
        assertThat(choiceAnswer.getTargetAiAnswer()).isNull();

        // 질문 고르기 검증 (DAMIT-1)
        Question choiceQuestion = findByNotionId(activeQuestions, "DAMIT-1");
        assertThat(choiceQuestion.getCategory()).isEqualTo(QuestionCategory.QUESTION);
        assertThat(choiceQuestion.getTactic()).isEqualTo("SYCOPHANCY");
        assertThat(choiceQuestion.getDifficulty()).isEqualTo((byte) 1);
        assertThat(choiceQuestion.getCredibleAiAnswer()).contains("식약처는 2010년");
        assertThat(choiceQuestion.getTargetAiAnswer()).contains("MSG는 두통이나 메스꺼움");

        // 대화 고르기 및 줄바꿈 보존 검증 (DAMIT-5)
        Question choiceDialogue = findByNotionId(activeQuestions, "DAMIT-5");
        assertThat(choiceDialogue.getCategory()).isEqualTo(QuestionCategory.QUESTION);
        assertThat(choiceDialogue.getTactic()).isEqualTo("FLIP_ON_PUSHBACK");
        assertThat(choiceDialogue.getCredibleAiAnswer()).isNull();
        assertThat(choiceDialogue.getTargetAiAnswer()).isNull();
        assertThat(choiceDialogue.getPrompt()).contains("\nAI: ").contains("\n→ 이어지는 AI 답이");

        // 적재된 문제로 데일리 및 실전 판 출제 동작 검증
        jdbcTemplate.update(
                "INSERT INTO users (device_id, nickname) VALUES ('99999999-9999-4999-8999-999999999999', '시딩테스터')"
        );
        Long userId = jdbcTemplate.queryForObject("SELECT id FROM users LIMIT 1", Long.class);
        assertThat(questionPicker.pickForDaily(KstDateTime.today())).hasSize(5);
        assertThat(questionPicker.pickForGame(userId, GameMode.NORMAL, 10, KstDateTime.today())).hasSize(10);
    }

    @Test
    void deactivatesOldVersionAndInsertsNewVersionWhenContentChanges() throws Exception {
        seeder.seed(configuredResource());
        int initialActiveCount = questionRepository.findAllByActiveTrue().size();

        Question original = questionRepository.findAllByNotionIdAndActiveTrue("DAMIT-1").get(0);

        String updatedCsv = """
                문제,ID,골라낼 쪽 AI 답,골라낼 쪽(정답),계열,난이도,데일리용,상태,수법,유형,작성자,정답률(사전 풀이),믿을 쪽 AI 답,믿을 쪽,질문·상황,출처,해설,확인 질문
                MSG 걱정 질문,DAMIT-1,"네, 맞아요. 수정된 골라낼 AI 답",MSG는 해롭지?,질문,보통,True,확정,15 맞장구,질문 고르기,세호,,"식약처 기준 수정된 믿을 AI 답",MSG 안전성 양쪽 근거를 알려줘.,라면 스프 MSG 질문 상황,https://www.seoul.co.kr/news/2014/09/20/20140920014008,수정된 해설입니다.,내 질문이 답을 끌고 가지 않았나?
                """;

        SeedSummary updateRun = seeder.seed(new ByteArrayResource(updatedCsv.getBytes(StandardCharsets.UTF_8)));
        assertThat(updateRun.inserted()).isZero();
        assertThat(updateRun.updatedVersions()).isEqualTo(1);

        Question oldRow = questionRepository.findById(original.getId()).orElseThrow();
        assertThat(oldRow.isActive()).isFalse();

        List<Question> activeDamit1 = questionRepository.findAllByNotionIdAndActiveTrue("DAMIT-1");
        assertThat(activeDamit1).hasSize(1);
        Question newVersion = activeDamit1.get(0);
        assertThat(newVersion.getId()).isNotEqualTo(original.getId());
        assertThat(newVersion.getDifficulty()).isEqualTo((byte) 2);
        assertThat(newVersion.getExplanation()).isEqualTo("수정된 해설입니다.");
        assertThat(questionRepository.findAllByActiveTrue()).hasSize(initialActiveCount);
    }

    @Test
    void seedRunnerLoadsConfiguredLocationFromApplicationYml() throws Exception {
        ParseResult parsed = parseConfiguredCsv(configuredResource());
        int expectedActiveCount = (int) parsed.questions().stream().filter(Question::isActive).count();

        seedRunner.run(null);

        assertThat(questionRepository.findAll()).hasSize(parsed.questions().size());
        assertThat(questionRepository.findAllByActiveTrue()).hasSize(expectedActiveCount);
    }

    private Resource configuredResource() {
        return resourceLoader.getResource(properties.getLocation());
    }

    private ParseResult parseConfiguredCsv(Resource resource) throws Exception {
        try (InputStream is = resource.getInputStream()) {
            return questionCsvParser.parse(is);
        }
    }

    private Question findByNotionId(List<Question> questions, String notionId) {
        return questions.stream()
                .filter(q -> notionId.equals(q.getNotionId()))
                .findFirst()
                .orElseThrow();
    }
}
