package com.select.product.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class CollectTaskDTO {
    @NotBlank(message = "任务名称不能为空")
    private String taskName;
    @NotBlank(message = "平台不能为空")
    private String platform;
    @NotBlank(message = "关键词不能为空")
    private String keyword;
    @NotNull(message = "最大采集数量不能为空")
    private Integer maxCount;

    /** 采集筛选：只采集支持一件代发的商品 */
    private Boolean filterDropShipping;
    /** 采集筛选：只采集支持48小时发货的商品 */
    private Boolean filterDelivery48h;
    /** 采集筛选：只采集包邮商品 */
    private Boolean filterFreeShipping;
    /** 采集筛选：只采集支持抖音面单的商品 */
    private Boolean filterDouyinSheet;
}



