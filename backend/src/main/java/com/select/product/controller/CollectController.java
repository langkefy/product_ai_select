package com.select.product.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.select.product.dto.CollectTaskDTO;
import com.select.product.dto.Result;
import com.select.product.entity.CollectTask;
import com.select.product.service.CollectService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@Api(tags = "采集任务管理")
@RestController
@RequestMapping("/api/collect")
@RequiredArgsConstructor
public class CollectController {

    private final CollectService collectService;

    @ApiOperation("创建采集任务")
    @PostMapping("/task")
    public Result<CollectTask> createTask(@RequestBody @Validated CollectTaskDTO dto) {
        return Result.ok(collectService.createTask(dto));
    }

    @ApiOperation("执行采集任务")
    @PostMapping("/task/{id}/execute")
    public Result<CollectTask> executeTask(@PathVariable Long id) {
        return Result.ok(collectService.executeTask(id));
    }

    @ApiOperation("同步任务状态")
    @GetMapping("/task/{id}/sync")
    public Result<CollectTask> syncTaskStatus(@PathVariable Long id) {
        return Result.ok(collectService.syncTaskStatus(id));
    }

    @ApiOperation("获取任务列表")
    @GetMapping("/tasks")
    public Result<Page<CollectTask>> getTaskList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(collectService.getTaskList(page, size));
    }

    @ApiOperation("重试失败任务")
    @PostMapping("/task/{id}/retry")
    public Result<CollectTask> retryTask(@PathVariable Long id) {
        return Result.ok(collectService.retryTask(id));
    }

    @ApiOperation("补全缺少发货地的商品详情（调用item_get接口）")
    @PostMapping("/fill-detail")
    public Result<Map<String, Object>> fillMissingDetail() {
        return Result.ok(collectService.fillMissingDetail());
    }

    @ApiOperation("按任务ID补全商品详情（发货地/售后/面单/库存同步等）")
    @PostMapping("/task/{id}/fill-detail")
    public Result<Map<String, Object>> fillMissingDetailByTask(@PathVariable Long id) {
        return Result.ok(collectService.fillMissingDetailByTask(id));
    }

    @ApiOperation("清除某个采集任务的所有商品数据")
    @DeleteMapping("/task/{id}/products")
    public Result<Map<String, Object>> clearTaskProducts(@PathVariable Long id) {
        return Result.ok(collectService.clearTaskProducts(id));
    }
}

