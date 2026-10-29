package com.zhiguang.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class SignInResultDto {
    private LocalDate signDate;
    private Integer continuousDays;
    private Integer rewardPoints;
    private Integer rewardExperience;
    private Boolean supplement;
    private Integer totalPoints;
    private Integer totalExperience;
    private Integer level;
}
