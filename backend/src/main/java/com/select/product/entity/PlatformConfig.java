package com.select.product.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("platform_config")
public class PlatformConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 平台名称，如 OpenClaw、99API */
    private String name;

    /** 平台类型: OPENCLAW / API99 */
    private String platformType;

    /** API 地址 */
    private String apiUrl;

    /** API Key */
    private String apiKey;

    /** API Secret（OpenClaw 使用） */
    private String apiSecret;

    /** OAuth2 Access Token（1688开放平台 ALI_OPEN 使用，可选） */
    private String accessToken;

    /** 是否为当前激活的采集平台 */
    private Integer isActive;

    /** 备注 */
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}

