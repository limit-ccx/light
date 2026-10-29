package com.zhiguang.dto.treehole;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TreeHolePostDTO {

    @NotBlank(message = "内容不能为空")
    @Size(min = 5, max = 2000, message = "内容长度需在 5-2000 字之间")
    private String content;

    @Size(max = 30, message = "情绪标签长度不能超过 30 个字符")
    private String emotionTag;
}
