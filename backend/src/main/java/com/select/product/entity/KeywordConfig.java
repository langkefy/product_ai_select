package com.select.product.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("keyword_config")
public class KeywordConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关键词 */
    private String keyword;

    /** 平台 */
    private String platform;

    /** 品类 */
    private String category;

    /** 优先级 1-10 */
    private Integer priority;

    /** 是否启用 */
    private Integer enabled;

    /** Cron表达式 */
    private String cronExpr;

    /** 每次采集数量 */
    private Integer maxCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}

