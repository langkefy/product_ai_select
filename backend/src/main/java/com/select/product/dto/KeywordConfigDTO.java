package com.select.product.dto;

import lombok.Data;

@Data
public class KeywordConfigDTO {
    private String keyword;
    private String platform;
    private String category;
    private Integer priority;
    private Integer enabled;
    private String cronExpr;
    private Integer maxCount;
}

