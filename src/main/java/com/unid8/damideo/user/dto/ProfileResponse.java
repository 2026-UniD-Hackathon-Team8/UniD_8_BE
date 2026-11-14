package com.unid8.damideo.user.dto;

public record ProfileResponse(String nickname, Streak streak, int bestWinStreak, WeakTypeView weakType) {

    public record Streak(int current, int longest, boolean completedToday) {
    }

    /**
     * @param status ANALYZING이면 code, name, weakCategory, strongCategory, checkQuestion은 null
     */
    public record WeakTypeView(String status,
                               String code,
                               String name,
                               String weakCategory,
                               String strongCategory,
                               String checkQuestion,
                               Progress progress) {
    }

    public record Progress(int solved, int required) {
    }
}
