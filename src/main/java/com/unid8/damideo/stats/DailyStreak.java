package com.unid8.damideo.stats;

import java.time.LocalDate;

/**
 * 데일리 연속 기록.
 * 0으로 되돌리는 배치 없이, 조회할 때 마지막 완료일이 어제보다 이전이면 0으로 보여 준다.
 */
public final class DailyStreak {

    private DailyStreak() {
    }

    /**
     * 화면에 보여 줄 현재 연속 일수.
     */
    public static int effective(int currentStreak, LocalDate lastCompletedDate, LocalDate today) {
        if (lastCompletedDate == null || lastCompletedDate.isBefore(today.minusDays(1))) {
            return 0;
        }
        return currentStreak;
    }

    /**
     * 오늘 데일리를 완료했을 때의 새 기록. 같은 날 두 번 불려도 결과가 같다.
     */
    public static Record complete(int currentStreak, int longestStreak, LocalDate lastCompletedDate, LocalDate today) {
        if (today.equals(lastCompletedDate)) {
            return new Record(currentStreak, longestStreak, lastCompletedDate);
        }
        int next = today.minusDays(1).equals(lastCompletedDate) ? currentStreak + 1 : 1;
        return new Record(next, Math.max(longestStreak, next), today);
    }

    public record Record(int currentStreak, int longestStreak, LocalDate lastCompletedDate) {
    }
}
