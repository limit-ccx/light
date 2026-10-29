package com.zhiguang.dto;

import lombok.Data;
import java.util.List;

@Data
public class RagAskRequest {
    private String question;
    private List<Long> itemIds; // 选中的知识库条目ID列表
}
