package com.zhiguang.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CreatorApplicationDto {

    @NotBlank(message = "认证方向不能为空")
    @Size(max = 50, message = "认证方向长度不能超过50")
    private String direction;

    @Size(max = 1000, message = "个人简介长度不能超过1000")
    private String bio;

    @NotNull(message = "作品链接不能为空")
    @Size(max = 10, message = "作品链接最多10个")
    private List<String> works;
}
