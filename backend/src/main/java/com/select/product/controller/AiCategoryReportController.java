package com.select.product.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.select.product.dto.Result;
import com.select.product.entity.AiCategoryReport;
import com.select.product.service.AiCategoryReportService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Api(tags = "AI选品分析报告")
@RestController
@RequestMapping("/api/ai-analysis")
@RequiredArgsConstructor
public class AiCategoryReportController {

    private final AiCategoryReportService reportService;

    @ApiOperation("生成AI分析报告（入库并返回）")
    @PostMapping("/generate")
    public Result<AiCategoryReport> generate(
            @RequestParam(defaultValue = "抖音") String platform,
            @RequestParam(required = false) String category) {
        AiCategoryReport report = reportService.generateReport(platform, category);
        return Result.ok(report);
    }

    @ApiOperation("分页查询历史报告")
    @GetMapping("/list")
    public Result<Page<AiCategoryReport>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(reportService.listReports(page, size));
    }

    @ApiOperation("查询单份报告详情")
    @GetMapping("/{id}")
    public Result<AiCategoryReport> detail(@PathVariable Long id) {
        AiCategoryReport report = reportService.getReport(id);
        if (report == null) return Result.fail(404, "报告不存在");
        return Result.ok(report);
    }
}

