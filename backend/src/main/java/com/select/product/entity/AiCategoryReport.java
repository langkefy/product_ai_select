package com.select.product.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ai_category_report")
public class AiCategoryReport {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 报告标题 */
    private String title;

    /** 分析平台，如 抖音 */
    private String platform;

    /** 分析品类（为空表示全品类） */
    private String category;

    /** 当前热门分类 JSON */
    private String hotCategories;

    /** 即将热门分类 JSON */
    private String risingCategories;

    /** 分类选品推荐 JSON */
    private String productRecommendations;

    /** 上架建议 JSON */
    private String listingAdvice;

    /** 推广分析 JSON */
    private String promotionAnalysis;

    /** 营销推广流程 JSON */
    private String marketingFlow;

    /** 是否由AI生成（0=规则引擎, 1=AI）*/
    private Integer aiGenerated;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}

