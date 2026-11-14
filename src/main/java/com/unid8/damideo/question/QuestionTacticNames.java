package com.unid8.damideo.question;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 수법 enum 코드에 대응하는 한글 표시명을 제공한다.
 */
public final class QuestionTacticNames {

    private static final Map<String, String> NAMES = Arrays.stream(QuestionTactic.values())
            .collect(Collectors.toUnmodifiableMap(QuestionTactic::name, QuestionTactic::displayName));

    private QuestionTacticNames() {
    }

    public static String resolve(String tacticCode) {
        if (tacticCode == null) {
            return "";
        }
        return NAMES.getOrDefault(tacticCode, tacticCode);
    }
}
