package com.select.product.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("collect_task")
public class CollectTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String taskName;
    private String platform;
    private String keyword;

    /** 任务状态: PENDING/RUNNING/SUCCESS/FAILED */
    private String status;

    /** OpenClaw外部任务ID */
    private String openClawTaskId;

    /** 最大采集数量 */
    private Integer maxCount;

    /** 总商品数 */
    private Integer totalCount;

    /** 成功入库数 */
    private Integer successCount;

    /** 错误信息 */
    private String errorMsg;

    /** 采集筛选条件：一件代发 */
    private Integer filterDropShipping;
    /** 采集筛选条件：48小时发货 */
    private Integer filterDelivery48h;
    /** 采集筛选条件：包邮 */
    private Integer filterFreeShipping;
    /** 采集筛选条件：抖音面单 */
    private Integer filterDouyinSheet;

    /** 补全详情处理数量 */
    private Integer fillDetailCount;

    /** 补全详情成功数量 */
    private Integer fillDetailSuccess;

    /** 补全详情失败数量 */
    private Integer fillDetailFailed;

    /** 补全详情状态: null/RUNNING/DONE/FAILED */
    private String fillDetailStatus;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}

