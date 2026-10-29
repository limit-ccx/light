package com.zhiguang.dto.treehole;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TreeHoleReplyDTO {

    @NotNull(message = "帖子ID不能为空")
    private Long postId;

    @NotBlank(message = "回复内容不能为空")
    @Size(min = 5, max = 1000, message = "回复内容长度需在 5-1000 字之间")
    private String content;
}
