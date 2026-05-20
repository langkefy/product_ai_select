package com.select.product.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.select.product.entity.AiCategoryReport;

public interface AiCategoryReportService {

    /**
     * 触发AI分析：当前热门分类、即将热门分类、分类选品推荐、上架建议、抖音推广分析、营销流程
     * 结果入库并返回报告实体
     */
    AiCategoryReport generateReport(String platform, String category);

    /** 分页查询历史报告（最新在前） */
    Page<AiCategoryReport> listReports(int page, int size);

    /** 查询单份报告 */
    AiCategoryReport getReport(Long id);
}

