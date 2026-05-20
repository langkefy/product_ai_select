package com.select.product.dto;

import lombok.Data;
import java.util.List;

@Data
public class AIAnalysisDTO {
    private Long productId;
    private List<Long> productIds;
    /** analysisType: BASIC/DEEP */
    private String analysisType = "BASIC";
}

