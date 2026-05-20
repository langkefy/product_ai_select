package com.select.product.dto;

import lombok.Data;

@Data
public class CategoryTrendVO {
    /** 品类 */
    private String category;
    /** 日期 */
    private String statDate;
    /** 商品数量 */
    private Long productCount;
    /** 平均AI评分 */
    private Double avgAiScore;
    /** 平均销量 */
    private Double avgSales;
}

