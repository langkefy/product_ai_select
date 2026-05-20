package com.select.product.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("daily_stats")
public class DailyStats {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long productId;
    private LocalDate statDate;
    private Long sales;
    private Long views;
    private BigDecimal rating;
    private Integer rank;
    private BigDecimal priceChange;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}

