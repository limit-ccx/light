package com.zhiguang.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiWriteRequestDto {

    @NotBlank
    @Length(max = 10000)
    private String content;

    private String title;
}
