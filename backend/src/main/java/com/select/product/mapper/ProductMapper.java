package com.select.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.select.product.dto.CategoryTrendVO;
import com.select.product.dto.ProductQueryDTO;
import com.select.product.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    Page<Product> selectRankingList(Page<Product> page, @Param("dto") ProductQueryDTO dto);

    List<Product> selectTopByAiScore(@Param("limit") int limit, @Param("platform") String platform);

    List<String> selectDistinctCategories();

    List<CategoryTrendVO> selectCategoryTrend(@Param("category") String category, @Param("days") int days);

    /** 最近N天每日采集商品数量 */
    List<Map<String, Object>> selectDailyCollectCount(@Param("days") int days);

    /** 今日采集商品数量 */
    Long selectTodayCollectCount();

    /** AI评分分布: 上架/测试/放弃/未分析 */
    List<Map<String, Object>> selectVerdictDistribution();
}

