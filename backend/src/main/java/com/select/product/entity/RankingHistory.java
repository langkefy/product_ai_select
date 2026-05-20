package com.select.product.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("ranking_history")
public class RankingHistory {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 商品ID */
    private Long productId;

    /** 排名类型: TODAY/WEEK/MONTH */
    private String rankType;

    /** 排名位置 */
    private Integer rankPosition;

    /** AI评分快照 */
    private Integer score;

    /** 销量快照 */
    private Long sales;

    /** 统计日期 */
    private LocalDate statDate;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}

