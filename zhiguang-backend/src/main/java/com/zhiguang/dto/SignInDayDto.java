package com.zhiguang.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class SignInDayDto {
    private LocalDate date;
    private Boolean signed;
    private Boolean supplement;
    private Boolean canSign;
    private Boolean canSupplement;
    private Integer dayOfMonth;
    private Integer rewardPoints;
    private Integer rewardExperience;
}
