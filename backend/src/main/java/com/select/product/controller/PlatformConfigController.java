package com.select.product.controller;

import com.select.product.dto.Result;
import com.select.product.entity.PlatformConfig;
import com.select.product.service.PlatformConfigService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "采集平台配置")
@RestController
@RequestMapping("/api/platform-config")
@RequiredArgsConstructor
public class PlatformConfigController {

    private final PlatformConfigService service;

    @ApiOperation("获取所有平台配置")
    @GetMapping
    public Result<List<PlatformConfig>> list() {
        return Result.ok(service.listAll());
    }

    @ApiOperation("获取当前激活平台")
    @GetMapping("/active")
    public Result<PlatformConfig> active() {
        return Result.ok(service.getActive());
    }

    @ApiOperation("新增平台配置")
    @PostMapping
    public Result<PlatformConfig> create(@RequestBody PlatformConfig config) {
        return Result.ok(service.save(config));
    }

    @ApiOperation("更新平台配置")
    @PutMapping("/{id}")
    public Result<PlatformConfig> update(@PathVariable Long id, @RequestBody PlatformConfig config) {
        return Result.ok(service.update(id, config));
    }

    @ApiOperation("激活指定平台")
    @PostMapping("/{id}/activate")
    public Result<Void> activate(@PathVariable Long id) {
        service.activate(id);
        return Result.ok(null);
    }

    @ApiOperation("删除平台配置")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return Result.ok(null);
    }
}

