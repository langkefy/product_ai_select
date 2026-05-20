package com.select.product.service;

import com.select.product.entity.Product;
import java.util.List;
import java.util.Map;

public interface AIService {
    Product analyzeProduct(Long productId);
    List<Product> batchAnalyze(List<Long> productIds);
    String generateRankingReport(String platform, String category);
    /** 分析所有未分析商品，结果按决策排序（上架>测试>放弃） */
    List<Product> analyzeAllUnanalyzed();

    /**
     * 根据品类 + 用户自定义需求，结合当前季节/时间，用AI推荐选品关键词
     * @param category 品类名称（如：女装、生活用品），为空则走默认全品类
     * @param customInput 用户自定义补充需求，可为空
     */
    List<Map<String, String>> recommendKeywords(String category, String customInput);
}

