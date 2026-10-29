package com.zhiguang.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PolishResponseDto {

    /** 原始内容 */
    private String originalContent;

    /** 润色后内容 */
    private String polishedContent;

    /** 使用的润色模式 */
    private String mode;

    /** 使用的模型名称 */
    private String model;
}
