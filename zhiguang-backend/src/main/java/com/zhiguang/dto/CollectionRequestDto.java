package com.zhiguang.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CollectionRequestDto {

    @NotBlank(message = "合集名称不能为空")
    @Length(max = 100)
    private String name;

    private String description;
}
