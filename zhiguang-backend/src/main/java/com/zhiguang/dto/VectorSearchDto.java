package com.zhiguang.dto;

import lombok.Data;
import java.util.List;

@Data
public class VectorSearchDto {
    private Long id;
    private String title;
    private Double score;
    private String type;
    private Long entityId;
    private List<Double> vector;
}
