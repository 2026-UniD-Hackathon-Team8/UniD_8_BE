package com.unid8.damideo.user.dto;

import java.time.LocalDate;
import java.util.List;

public record DailyCalendarResponse(List<Day> days) {

    public record Day(LocalDate date, boolean completed, int correctCount) {
    }
}
