package com.unid8.damideo.question;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

/**
 * 노션 문제은행 CSV를 파싱해 {@link Question} 목록으로 변환한다.
 * 문제, 작성자, 정답률(사전 풀이) 컬럼은 적재 대상에서 제외한다.
 */
@Slf4j
@Component
public class QuestionCsvParser {

    private static final String COL_ID = "ID";
    private static final String COL_CATEGORY = "계열";
    private static final String COL_TACTIC = "수법";
    private static final String COL_TYPE = "유형";
    private static final String COL_PROMPT = "질문·상황";
    private static final String COL_CREDIBLE_CONTENT = "믿을 쪽";
    private static final String COL_TARGET_CONTENT = "골라낼 쪽(정답)";
    private static final String COL_CREDIBLE_AI_ANSWER = "믿을 쪽 AI 답";
    private static final String COL_TARGET_AI_ANSWER = "골라낼 쪽 AI 답";
    private static final String COL_EXPLANATION = "해설";
    private static final String COL_CHECK_QUESTION = "확인 질문";
    private static final String COL_SOURCE = "출처";
    private static final String COL_DIFFICULTY = "난이도";
    private static final String COL_DAILY_ELIGIBLE = "데일리용";
    private static final String COL_STATUS = "상태";

    private static final List<String> REQUIRED_HEADERS = List.of(
            COL_ID,
            COL_CATEGORY,
            COL_TACTIC,
            COL_TYPE,
            COL_PROMPT,
            COL_CREDIBLE_CONTENT,
            COL_TARGET_CONTENT,
            COL_CREDIBLE_AI_ANSWER,
            COL_TARGET_AI_ANSWER,
            COL_EXPLANATION,
            COL_CHECK_QUESTION,
            COL_SOURCE,
            COL_DIFFICULTY,
            COL_DAILY_ELIGIBLE,
            COL_STATUS
    );

    private static final List<String> COMMON_REQUIRED_COLUMNS = List.of(
            COL_ID,
            COL_CATEGORY,
            COL_TACTIC,
            COL_TYPE,
            COL_PROMPT,
            COL_CREDIBLE_CONTENT,
            COL_TARGET_CONTENT,
            COL_EXPLANATION,
            COL_CHECK_QUESTION,
            COL_SOURCE,
            COL_DIFFICULTY,
            COL_DAILY_ELIGIBLE
    );

    private static final Set<String> ACTIVE_STATUSES = Set.of("출처 확인", "확정");

    /**
     * UTF-8 CSV 입력 스트림을 파싱해 적재 가능한 문제와 누락으로 제외된 행 정보를 반환한다.
     */
    public ParseResult parse(InputStream inputStream) throws IOException {
        try (Reader reader = stripUtf8Bom(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
             CSVParser csvParser = CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .build()
                     .parse(reader)) {

            validateHeaders(csvParser.getHeaderMap());

            List<Question> questions = new ArrayList<>();
            List<SkippedRow> skippedRows = new ArrayList<>();

            for (CSVRecord record : csvParser) {
                List<String> missingColumns = findMissingCommonColumns(record);
                if (!missingColumns.isEmpty()) {
                    String notionId = safeTrim(record.get(COL_ID));
                    log.warn("필수 컬럼 누락으로 문제를 제외합니다. row={}, notionId={}, missing={}",
                            record.getRecordNumber(), notionId, missingColumns);
                    skippedRows.add(new SkippedRow(record.getRecordNumber(), notionId, missingColumns));
                    continue;
                }

                QuestionType questionType = QuestionType.fromCsvValue(record.get(COL_TYPE));
                if (questionType == QuestionType.CHOICE_QUESTION) {
                    List<String> missingAiCols = findMissingQuestionChoiceColumns(record);
                    if (!missingAiCols.isEmpty()) {
                        String notionId = safeTrim(record.get(COL_ID));
                        log.warn("질문 고르기 필수 AI 답 누락으로 문제를 제외합니다. row={}, notionId={}, missing={}",
                                record.getRecordNumber(), notionId, missingAiCols);
                        skippedRows.add(new SkippedRow(record.getRecordNumber(), notionId, missingAiCols));
                        continue;
                    }
                }

                questions.add(toQuestion(record, questionType));
            }

            return new ParseResult(List.copyOf(questions), List.copyOf(skippedRows));
        }
    }

    private Question toQuestion(CSVRecord record, QuestionType questionType) {
        String notionId = normalizeText(record.get(COL_ID));
        QuestionCategory category = QuestionCategory.fromCsvValue(record.get(COL_CATEGORY));
        QuestionTactic tactic = QuestionTactic.fromCsvValue(record.get(COL_TACTIC));
        String prompt = normalizeText(record.get(COL_PROMPT));
        String credibleContent = normalizeText(record.get(COL_CREDIBLE_CONTENT));
        String targetContent = normalizeText(record.get(COL_TARGET_CONTENT));
        String explanation = normalizeText(record.get(COL_EXPLANATION));
        String checkQuestion = normalizeText(record.get(COL_CHECK_QUESTION));
        String source = normalizeText(record.get(COL_SOURCE));
        byte difficulty = parseDifficulty(record.get(COL_DIFFICULTY));
        boolean dailyEligible = parseDailyEligible(record.get(COL_DAILY_ELIGIBLE));
        boolean active = parseActiveStatus(record.get(COL_STATUS));

        String credibleAiAnswer = null;
        String targetAiAnswer = null;
        if (questionType == QuestionType.CHOICE_QUESTION) {
            credibleAiAnswer = normalizeText(record.get(COL_CREDIBLE_AI_ANSWER));
            targetAiAnswer = normalizeText(record.get(COL_TARGET_AI_ANSWER));
        }

        return Question.create(
                notionId,
                category,
                tactic.name(),
                questionType,
                prompt,
                credibleContent,
                targetContent,
                credibleAiAnswer,
                targetAiAnswer,
                explanation,
                checkQuestion,
                source,
                difficulty,
                dailyEligible,
                active
        );
    }

    private void validateHeaders(Map<String, Integer> headerMap) {
        List<String> missingHeaders = REQUIRED_HEADERS.stream()
                .filter(header -> !headerMap.containsKey(header))
                .toList();
        if (!missingHeaders.isEmpty()) {
            throw new IllegalArgumentException("CSV 필수 헤더가 누락되었습니다: " + missingHeaders);
        }
    }

    private List<String> findMissingCommonColumns(CSVRecord record) {
        return COMMON_REQUIRED_COLUMNS.stream()
                .filter(col -> isBlank(record.get(col)))
                .toList();
    }

    private List<String> findMissingQuestionChoiceColumns(CSVRecord record) {
        List<String> missing = new ArrayList<>();
        if (isBlank(record.get(COL_CREDIBLE_AI_ANSWER))) {
            missing.add(COL_CREDIBLE_AI_ANSWER);
        }
        if (isBlank(record.get(COL_TARGET_AI_ANSWER))) {
            missing.add(COL_TARGET_AI_ANSWER);
        }
        return missing;
    }

    private byte parseDifficulty(String raw) {
        String trimmed = safeTrim(raw);
        return switch (trimmed) {
            case "쉬움", "1" -> (byte) 1;
            case "보통", "2" -> (byte) 2;
            case "어려움", "3" -> (byte) 3;
            default -> throw new IllegalArgumentException("알 수 없는 난이도 값입니다: " + raw);
        };
    }

    private boolean parseDailyEligible(String raw) {
        String trimmed = safeTrim(raw);
        if ("true".equalsIgnoreCase(trimmed)) {
            return true;
        }
        if ("false".equalsIgnoreCase(trimmed) || "no".equalsIgnoreCase(trimmed)) {
            return false;
        }
        throw new IllegalArgumentException("알 수 없는 데일리용 값입니다: " + raw);
    }

    private boolean parseActiveStatus(String raw) {
        String trimmed = safeTrim(raw);
        return ACTIVE_STATUSES.contains(trimmed);
    }

    private Reader stripUtf8Bom(Reader delegate) throws IOException {
        BufferedReader reader = new BufferedReader(delegate);
        reader.mark(1);
        int first = reader.read();
        if (first != '\uFEFF') {
            reader.reset();
        }
        return reader;
    }

    private String normalizeText(String value) {
        return value.replace("\r\n", "\n").trim();
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record ParseResult(
            List<Question> questions,
            List<SkippedRow> skippedRows
    ) {
    }

    public record SkippedRow(
            long recordNumber,
            String notionId,
            List<String> missingColumns
    ) {
    }
}
