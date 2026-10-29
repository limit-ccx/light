package com.zhiguang.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class SignInCalendarDto {
    private Integer year;
    private Integer month;
    private List<SignInDayDto> days;
    private Integer continuousDays;
    private Integer signedDays;
    private Integer supplementRemaining;
    private Boolean signedToday;
}
