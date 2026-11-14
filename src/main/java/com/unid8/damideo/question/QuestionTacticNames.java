package com.unid8.damideo.question;

import java.util.Map;

/**
 * 수법 enum 코드에 대응하는 한글 표시명을 제공한다.
 */
public final class QuestionTacticNames {

    private static final Map<String, String> NAMES = Map.ofEntries(
            // 근거
            Map.entry("FAKE_SOURCE", "가짜 출처"),
            Map.entry("DISTORTED_SOURCE", "출처 왜곡"),
            Map.entry("FAKE_QUOTE", "가짜 인용"),
            Map.entry("PLAUSIBLE_NUMBER", "그럴듯한 숫자"),
            Map.entry("SANDWICHED_LIE", "사실 사이 끼워넣기"),
            // 맥락
            Map.entry("MISSING_CONDITION", "조건 빼먹기"),
            Map.entry("WRONG_TARGET", "대상 바꿔치기"),
            Map.entry("OVERCONFIDENT", "단정적 말투"),
            // 논리
            Map.entry("FALSE_CAUSALITY", "인과 비약"),
            Map.entry("HASTY_GENERALIZATION", "성급한 일반화"),
            Map.entry("NUMBER_ILLUSION", "숫자 착시"),
            Map.entry("CALC_UNIT_ERROR", "계산·단위 착오"),
            // 시점
            Map.entry("OUTDATED_INFO", "철 지난 정보"),
            Map.entry("NONEXISTENT_UPDATE", "없는 최신 정보"),
            // 질문
            Map.entry("SYCOPHANCY", "맞장구"),
            Map.entry("FALSE_PREMISE", "틀린 전제 수용"),
            Map.entry("FLIP_ON_PUSHBACK", "반박하면 말 바꾸기")
    );

    private QuestionTacticNames() {
    }

    public static String resolve(String tacticCode) {
        if (tacticCode == null) {
            return "";
        }
        return NAMES.getOrDefault(tacticCode, tacticCode);
    }
}
