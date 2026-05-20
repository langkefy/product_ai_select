package com.select.product.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.select.product.dto.CategoryTrendVO;
import com.select.product.dto.ProductDetailVO;
import com.select.product.dto.ProductQueryDTO;
import com.select.product.dto.TrendQueryDTO;
import com.select.product.entity.DailyStats;
import com.select.product.entity.Product;

import java.util.List;
import java.util.Map;

public interface ProductService {
    Page<Product> pageQuery(ProductQueryDTO dto);
    Product getById(Long id);
    ProductDetailVO getProductDetail(Long id);
    Page<Product> getRankingList(ProductQueryDTO dto);
    List<Product> getTopByAiScore(int limit, String platform);
    List<DailyStats> getTrendData(TrendQueryDTO dto);
    Map<String, Object> getDashboardData();
    boolean removeById(Long id);
    List<String> getCategories();
    List<CategoryTrendVO> getCategoryTrend(String category, int days);
}



