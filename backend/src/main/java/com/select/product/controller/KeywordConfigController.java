package com.select.product.controller;

import com.select.product.dto.KeywordConfigDTO;
import com.select.product.dto.Result;
import com.select.product.entity.KeywordConfig;
import com.select.product.service.KeywordConfigService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api(tags = "关键词配置")
@RestController
@RequestMapping("/api/keywords")
@RequiredArgsConstructor
public class KeywordConfigController {

    private final KeywordConfigService keywordConfigService;

    @ApiOperation("获取所有关键词配置")
    @GetMapping
    public Result<List<KeywordConfig>> list() {
        return Result.ok(keywordConfigService.listAll());
    }

    @ApiOperation("创建关键词配置")
    @PostMapping
    public Result<KeywordConfig> create(@RequestBody KeywordConfigDTO dto) {
        return Result.ok(keywordConfigService.create(dto));
    }

    @ApiOperation("更新关键词配置")
    @PutMapping("/{id}")
    public Result<KeywordConfig> update(@PathVariable Long id, @RequestBody KeywordConfigDTO dto) {
        return Result.ok(keywordConfigService.update(id, dto));
    }

    @ApiOperation("删除关键词配置")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        keywordConfigService.delete(id);
        return Result.ok();
    }

    @ApiOperation("立即触发采集")
    @PostMapping("/{id}/trigger")
    public Result<Void> trigger(@PathVariable Long id) {
        keywordConfigService.triggerCollect(id);
        return Result.ok();
    }
}

