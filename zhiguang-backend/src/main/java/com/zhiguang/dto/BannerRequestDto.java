package com.zhiguang.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BannerRequestDto {
    private String title;
    @NotBlank(message = "图片地址不能为空")
    private String imageUrl;
    private String linkUrl;
    private String position = "HOME";
    private Integer sortOrder = 0;
}
