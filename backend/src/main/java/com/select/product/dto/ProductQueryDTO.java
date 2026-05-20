package com.select.product.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ProductQueryDTO {
    private Integer page = 1;
    private Integer size = 20;
    private String keyword;
    private String category;
    private String platform;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private BigDecimal minRating;
    /** sortField: sales/rating/aiScore/createTime/verdict */
    private String sortField = "createTime";
    /** sortOrder: asc/desc */
    private String sortOrder = "desc";
    /** AI决策筛选: 上架/测试/放弃 */
    private String verdict;
    /** 时间范围筛选 */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    /** 筛选：支持48小时内发货 (1=是) */
    private Integer deliveryIn48h;
    /** 筛选：支持抖音面单 (1=是) */
    private Integer douyinSheetSupport;
    /** 筛选：包邮（运费为0或null视为包邮，传1开启筛选） */
    private Integer freeShipping;
    /** 筛选：支持一件代发（minNum=1，传1开启筛选） */
    private Integer dropShipping;
}

