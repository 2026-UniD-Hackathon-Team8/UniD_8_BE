package com.unid8.damideo.stats;

import java.util.Arrays;

/**
 * 유형 카드. users.current_type에는 약한 계열 코드(EVIDENCE 등)나 DETECTIVE가 저장된다.
 */
public enum WeakType {

    EVIDENCE_TRUSTER("EVIDENCE", "근거 맹신형", "근거가 실제로 있나?"),
    CONTEXT_MISSER("CONTEXT", "맥락 놓침형", "빠진 조건·예외는 없나?"),
    LOGIC_ILLUSION("LOGIC", "논리 착시형", "따져 보면 말이 되나?"),
    TIME_CONFUSED("TIMELINE", "시점 착각형", "언제 기준인가?"),
    LED_ALONG("QUESTION", "끌려가기형", "내 질문이 답을 끌고 가지 않았나?"),
    DETECTIVE(WeaknessTypeJudge.DETECTIVE, "탐정형", null);

    private final String storedType;
    private final String displayName;
    private final String checkQuestion;

    WeakType(String storedType, String displayName, String checkQuestion) {
        this.storedType = storedType;
        this.displayName = displayName;
        this.checkQuestion = checkQuestion;
    }

    public static WeakType fromStoredType(String storedType) {
        return Arrays.stream(values())
                .filter(type -> type.storedType.equals(storedType))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("알 수 없는 유형: " + storedType));
    }

    /**
     * 약한 계열 코드. 탐정형이면 null.
     */
    public String weakCategory() {
        return this == DETECTIVE ? null : storedType;
    }

    public String displayName() {
        return displayName;
    }

    public String checkQuestion() {
        return checkQuestion;
    }
}
