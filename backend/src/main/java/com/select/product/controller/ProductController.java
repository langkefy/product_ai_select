package com.select.product.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.select.product.dto.AIAnalysisDTO;
import com.select.product.dto.ProductDetailVO;
import com.select.product.dto.ProductQueryDTO;
import com.select.product.dto.Result;
import com.select.product.entity.Product;
import com.select.product.service.AIService;
import com.select.product.service.ProductService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Api(tags = "商品管理")
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final AIService aiService;

    @ApiOperation("分页查询商品")
    @GetMapping
    public Result<Page<Product>> list(ProductQueryDTO dto) {
        return Result.ok(productService.pageQuery(dto));
    }

    @ApiOperation("获取所有品类")
    @GetMapping("/categories")
    public Result<List<String>> categories() {
        return Result.ok(productService.getCategories());
    }

    @ApiOperation("AI推荐选品关键词（结合当前季节/时间/品类）")
    @GetMapping("/recommend-keywords")
    public Result<List<Map<String, String>>> recommendKeywords(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String customInput) {
        return Result.ok(aiService.recommendKeywords(category, customInput));
    }

    @ApiOperation("商品详情（含趋势和排名）")
    @GetMapping("/{id}")
    public Result<ProductDetailVO> detail(@PathVariable Long id) {
        ProductDetailVO vo = productService.getProductDetail(id);
        if (vo == null) return Result.fail(404, "商品不存在");
        return Result.ok(vo);
    }

    @ApiOperation("AI分析商品")
    @PostMapping("/ai-analyze")
    public Result<?> aiAnalyze(@RequestBody AIAnalysisDTO dto) {
        if (dto.getProductId() != null) {
            return Result.ok(aiService.analyzeProduct(dto.getProductId()));
        }
        if (dto.getProductIds() != null && !dto.getProductIds().isEmpty()) {
            List<Product> products = aiService.batchAnalyze(dto.getProductIds());
            return Result.ok(products);
        }
        return Result.fail("请指定商品ID");
    }

    @ApiOperation("分析全部未分析商品（结果按决策排序：上架>测试>放弃）")
    @PostMapping("/ai-analyze-all")
    public Result<List<Product>> aiAnalyzeAll() {
        List<Product> results = aiService.analyzeAllUnanalyzed();
        return Result.ok(results);
    }

    @ApiOperation("删除商品")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        productService.removeById(id);
        return Result.ok();
    }
}
