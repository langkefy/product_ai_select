package com.select.product.controller;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.select.product.dto.ProductQueryDTO;
import com.select.product.dto.RankingExportDTO;
import com.select.product.dto.Result;
import com.select.product.entity.Product;
import com.select.product.entity.RankingHistory;
import com.select.product.service.AIService;
import com.select.product.service.ProductService;
import com.select.product.service.RankingHistoryService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Api(tags = "排行榜")
@RestController
@RequestMapping("/api/ranking")
@RequiredArgsConstructor
public class RankingController {

    private final ProductService productService;
    private final AIService aiService;
    private final RankingHistoryService rankingHistoryService;

    @ApiOperation("排行榜（多维度排序）")
    @GetMapping("/top")
    public Result<Page<Product>> top(ProductQueryDTO dto) {
        return Result.ok(productService.getRankingList(dto));
    }

    @ApiOperation("AI评分排行")
    @GetMapping("/ai-score")
    public Result<List<Product>> aiScoreRanking(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String platform) {
        return Result.ok(productService.getTopByAiScore(limit, platform));
    }

    @ApiOperation("生成AI排行分析报告")
    @GetMapping("/report")
    public Result<String> report(
            @RequestParam(required = false) String platform,
            @RequestParam(required = false) String category) {
        return Result.ok(aiService.generateRankingReport(platform, category));
    }

    @ApiOperation("导出排行榜Excel")
    @GetMapping("/export")
    public void export(
            @RequestParam(required = false) String platform,
            @RequestParam(defaultValue = "100") int limit,
            HttpServletResponse response) throws IOException {
        List<Product> products = productService.getTopByAiScore(limit, platform);
        List<RankingExportDTO> rows = new ArrayList<>();
        for (int i = 0; i < products.size(); i++) {
            Product p = products.get(i);
            RankingExportDTO dto = new RankingExportDTO();
            dto.setRank(i + 1);
            dto.setTitle(p.getTitle());
            dto.setPlatform(p.getPlatform());
            dto.setCategory(p.getCategory());
            dto.setPrice(p.getPrice());
            dto.setSales(p.getSales());
            dto.setRating(p.getRating());
            dto.setAiScore(p.getAiScore());
            dto.setAiAnalysis(p.getAiAnalysis());
            rows.add(dto);
        }
        String filename = URLEncoder.encode("排行榜.xlsx", StandardCharsets.UTF_8);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + filename);
        EasyExcel.write(response.getOutputStream(), RankingExportDTO.class).sheet("排行榜").doWrite(rows);
    }

    @ApiOperation("商品排名历史")
    @GetMapping("/history/{productId}")
    public Result<List<RankingHistory>> history(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "TODAY") String rankType,
            @RequestParam(defaultValue = "30") int days) {
        return Result.ok(rankingHistoryService.getHistory(productId, rankType, days));
    }
}
