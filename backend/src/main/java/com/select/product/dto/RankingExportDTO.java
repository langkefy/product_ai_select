package com.select.product.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class RankingExportDTO {
    @ExcelProperty("排名")
    private Integer rank;

    @ExcelProperty("商品名称")
    private String title;

    @ExcelProperty("平台")
    private String platform;

    @ExcelProperty("品类")
    private String category;

    @ExcelProperty("价格(元)")
    private BigDecimal price;

    @ExcelProperty("销量")
    private Long sales;

    @ExcelProperty("评分")
    private BigDecimal rating;

    @ExcelProperty("AI评分")
    private Integer aiScore;

    @ExcelProperty("AI分析")
    private String aiAnalysis;
}

