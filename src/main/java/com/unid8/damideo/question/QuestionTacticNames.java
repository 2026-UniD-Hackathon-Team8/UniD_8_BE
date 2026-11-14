package com.unid8.damideo.question;

import java.util.Map;

/**
 * 수법 enum 코드에 대응하는 한글 표시명을 제공한다.
 */
public final class QuestionTacticNames {

    private static final Map<String, String> NAMES = Map.ofEntries(
            Map.entry("FAKE_SOURCE", "가짜 출처"),
            Map.entry("AUTHORITY_APPEAL", "권위 남용"),
            Map.entry("FAKE_STATISTICS", "가짜 통계"),
            Map.entry("OUT_OF_CONTEXT", "맥락 왜곡"),
            Map.entry("CHERRY_PICKING", "체리피킹"),
            Map.entry("FALSE_CAUSALITY", "인과 비약"),
            Map.entry("HASTY_GENERALIZATION", "성급한 일반화"),
            Map.entry("NUMBER_ILLUSION", "숫자 착시"),
            Map.entry("UNIT_ERROR", "계산·단위 착오"),
            Map.entry("TIME_CONFUSION", "시점 착각"),
            Map.entry("OUTDATED_INFO", "과거 정보"),
            Map.entry("LEADING_QUESTION", "유도 질문"),
            Map.entry("PREMISE_TRAP", "전제 함정"),
            Map.entry("GOALPOST_SHIFT", "말 바꾸기"),
            Map.entry("EVASIVE_ANSWER", "회피 답변")
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
