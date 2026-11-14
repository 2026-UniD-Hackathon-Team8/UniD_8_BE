package com.unid8.damideo.question;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 5개 계열에 속한 AI 할루시네이션·오류 수법 17종 Enum.
 */
public enum QuestionTactic {
    // 근거
    FAKE_SOURCE(1, "가짜 출처"),
    DISTORTED_SOURCE(2, "출처 왜곡"),
    FAKE_QUOTE(3, "가짜 인용"),
    PLAUSIBLE_NUMBER(4, "그럴듯한 숫자"),
    SANDWICHED_LIE(5, "사실 사이 끼워넣기"),
    // 맥락
    MISSING_CONDITION(6, "조건 빼먹기"),
    WRONG_TARGET(7, "대상 바꿔치기"),
    OVERCONFIDENT(8, "단정적 말투"),
    // 논리
    FALSE_CAUSALITY(9, "인과 비약"),
    HASTY_GENERALIZATION(10, "성급한 일반화"),
    NUMBER_ILLUSION(11, "숫자 착시"),
    CALC_UNIT_ERROR(12, "계산·단위 착오"),
    // 시점
    OUTDATED_INFO(13, "철 지난 정보"),
    NONEXISTENT_UPDATE(14, "없는 최신 정보"),
    // 질문
    SYCOPHANCY(15, "맞장구"),
    FALSE_PREMISE(16, "틀린 전제 수용"),
    FLIP_ON_PUSHBACK(17, "반박하면 말 바꾸기");

    private static final Pattern NUMBERED_TACTIC_PATTERN = Pattern.compile("^(\\d+)\\s+(.+)$");

    private final int number;
    private final String displayName;

    QuestionTactic(int number, String displayName) {
        this.number = number;
        this.displayName = displayName;
    }

    public int number() {
        return number;
    }

    public String displayName() {
        return displayName;
    }

    /**
     * CSV의 수법 값(예: "15 맞장구", "맞장구", "SYCOPHANCY")을 {@link QuestionTactic}으로 변환한다.
     */
    public static QuestionTactic fromCsvValue(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("수법 값이 비어 있습니다.");
        }
        String trimmed = raw.trim();
        Matcher matcher = NUMBERED_TACTIC_PATTERN.matcher(trimmed);
        if (matcher.matches()) {
            int parsedNumber = Integer.parseInt(matcher.group(1));
            String parsedName = matcher.group(2).trim();
            QuestionTactic byNumber = Arrays.stream(values())
                    .filter(tactic -> tactic.number == parsedNumber)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("알 수 없는 수법 번호입니다: " + raw));
            if (!byNumber.displayName.equals(parsedName)) {
                throw new IllegalArgumentException(
                        "수법 번호와 이름이 일치하지 않습니다: " + raw + " (기대: " + byNumber.displayName + ")"
                );
            }
            return byNumber;
        }

        return Arrays.stream(values())
                .filter(tactic -> tactic.displayName.equals(trimmed) || tactic.name().equalsIgnoreCase(trimmed))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("알 수 없는 수법입니다: " + raw));
    }
}
