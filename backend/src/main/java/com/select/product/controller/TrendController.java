package com.select.product.controller;

import com.select.product.dto.CategoryTrendVO;
import com.select.product.dto.Result;
import com.select.product.dto.TrendQueryDTO;
import com.select.product.entity.DailyStats;
import com.select.product.service.ProductService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Api(tags = "趋势分析")
@RestController
@RequestMapping("/api/trend")
@RequiredArgsConstructor
public class TrendController {

    private final ProductService productService;

    @ApiOperation("单商品趋势")
    @GetMapping("/product/{id}")
    public Result<List<DailyStats>> productTrend(@PathVariable Long id, TrendQueryDTO dto) {
        dto.setProductId(id);
        return Result.ok(productService.getTrendData(dto));
    }

    @ApiOperation("看板数据")
    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard() {
        return Result.ok(productService.getDashboardData());
    }

    @ApiOperation("品类趋势")
    @GetMapping("/category")
    public Result<List<CategoryTrendVO>> categoryTrend(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "30") int days) {
        return Result.ok(productService.getCategoryTrend(category, days));
    }
}
