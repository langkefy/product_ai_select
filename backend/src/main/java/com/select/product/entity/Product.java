package com.select.product.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("product")
public class Product {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 1688商品ID (用于去重) */
    private String numIid;

    /** 商品标题 */
    private String title;

    /** 价格 */
    private BigDecimal price;

    /** 代发价 */
    private BigDecimal agentPrice;

    /** 起批量 */
    private Integer minNum;

    /** 发货地 */
    private String location;

    /** 销量 */
    private Long sales;

    /** 评分 */
    private BigDecimal rating;

    /** 品类 */
    private String category;

    /** 平台: 1688/taobao/jd/pdd */
    private String platform;

    /** 商品图片URL */
    private String imageUrl;

    /** 商品详情链接 */
    private String detailUrl;

    /** AI评分 (0-100) */
    private Integer aiScore;

    /** AI决策: 上架/测试/放弃 */
    private String verdict;

    /** AI建议售价 */
    private BigDecimal suggestedPrice;

    /** 平台建议零售价（来自item_get接口 suggestive_price） */
    private BigDecimal platformSuggestPrice;

    /** 参考物流费（来自item_get接口 express_fee） */
    private BigDecimal shippingFee;

    /** 是否支持48小时内发货 (0否/1是) */
    @TableField("delivery_in_48h")
    private Integer deliveryIn48h;

    /** 是否支持全包售后（退换货由供应商承担）(0否/1是) */
    @TableField("full_after_sales")
    private Integer fullAfterSales;

    /** 是否支持抖音面单 (0否/1是) */
    @TableField("douyin_sheet_support")
    private Integer douyinSheetSupport;

    /** 是否支持自动同步库存 (0否/1是) */
    @TableField("auto_sync_stock")
    private Integer autoSyncStock;

    /** AI风险提示 */
    private String risk;

    /** AI分析文本 */
    private String aiAnalysis;

    /** AI生成的吸引人新标题 */
    private String aiTitle;

    /** 采集日期 (用于去重) */
    private LocalDate collectDate;

    /** 采集时间 */
    private LocalDateTime collectTime;

    /** 所属采集任务ID */
    private Long collectTaskId;

    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
