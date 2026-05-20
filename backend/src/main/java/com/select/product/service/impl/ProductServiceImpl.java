package com.select.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.select.product.dto.CategoryTrendVO;
import com.select.product.dto.ProductDetailVO;
import com.select.product.dto.ProductQueryDTO;
import com.select.product.dto.TrendQueryDTO;
import com.select.product.entity.CollectTask;
import com.select.product.entity.DailyStats;
import com.select.product.entity.Product;
import com.select.product.mapper.CollectTaskMapper;
import com.select.product.mapper.DailyStatsMapper;
import com.select.product.mapper.ProductMapper;
import com.select.product.mapper.RankingHistoryMapper;
import com.select.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductMapper productMapper;
    private final DailyStatsMapper dailyStatsMapper;
    private final CollectTaskMapper collectTaskMapper;
    private final RankingHistoryMapper rankingHistoryMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public Page<Product> pageQuery(ProductQueryDTO dto) {
        LambdaQueryWrapper<Product> wrapper = buildWrapper(dto);

        if ("verdict".equals(dto.getSortField())) {
            // 单独执行count避免last()ORDER BY影响分页计数
            long total = productMapper.selectCount(buildWrapper(dto));
            Page<Product> page = new Page<>(dto.getPage(), dto.getSize(), false);
            page.setTotal(total);
            wrapper.last("ORDER BY CASE verdict WHEN '上架' THEN 0 WHEN '测试' THEN 1 WHEN '放弃' THEN 2 ELSE 3 END ASC, create_time DESC");
            return productMapper.selectPage(page, wrapper);
        }

        Page<Product> page = new Page<>(dto.getPage(), dto.getSize());
        boolean asc = "asc".equalsIgnoreCase(dto.getSortOrder());
        switch (dto.getSortField()) {
            case "sales":   if (asc) wrapper.orderByAsc(Product::getSales);   else wrapper.orderByDesc(Product::getSales);   break;
            case "rating":  if (asc) wrapper.orderByAsc(Product::getRating);  else wrapper.orderByDesc(Product::getRating);  break;
            case "aiScore": if (asc) wrapper.orderByAsc(Product::getAiScore); else wrapper.orderByDesc(Product::getAiScore); break;
            default:        wrapper.orderByDesc(Product::getCreateTime); break;
        }
        return productMapper.selectPage(page, wrapper);
    }

    /** 构建公共查询条件（不含排序） */
    private LambdaQueryWrapper<Product> buildWrapper(ProductQueryDTO dto) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(dto.getKeyword()))   wrapper.like(Product::getTitle, dto.getKeyword());
        if (StringUtils.hasText(dto.getPlatform()))  wrapper.eq(Product::getPlatform, dto.getPlatform());
        if (StringUtils.hasText(dto.getCategory()))  wrapper.eq(Product::getCategory, dto.getCategory());
        if (dto.getMinPrice()  != null) wrapper.ge(Product::getPrice,  dto.getMinPrice());
        if (dto.getMaxPrice()  != null) wrapper.le(Product::getPrice,  dto.getMaxPrice());
        if (dto.getMinRating() != null) wrapper.ge(Product::getRating, dto.getMinRating());
        if (StringUtils.hasText(dto.getVerdict()))   wrapper.eq(Product::getVerdict, dto.getVerdict());
        if (dto.getStartDate() != null) wrapper.ge(Product::getCollectDate, dto.getStartDate());
        if (dto.getEndDate()   != null) wrapper.le(Product::getCollectDate, dto.getEndDate());
        // 48小时发货
        if (Integer.valueOf(1).equals(dto.getDeliveryIn48h())) wrapper.eq(Product::getDeliveryIn48h, 1);
        // 抖音面单
        if (Integer.valueOf(1).equals(dto.getDouyinSheetSupport())) wrapper.eq(Product::getDouyinSheetSupport, 1);
        // 包邮：运费为0或null
        if (Integer.valueOf(1).equals(dto.getFreeShipping()))
            wrapper.and(w -> w.isNull(Product::getShippingFee).or().eq(Product::getShippingFee, java.math.BigDecimal.ZERO));
        // 一件代发：minNum=1
        if (Integer.valueOf(1).equals(dto.getDropShipping())) wrapper.eq(Product::getMinNum, 1);
        return wrapper;
    }

    @Override
    public Product getById(Long id) {
        return productMapper.selectById(id);
    }

    @Override
    public Page<Product> getRankingList(ProductQueryDTO dto) {
        Page<Product> page = new Page<>(dto.getPage(), dto.getSize());
        return productMapper.selectRankingList(page, dto);
    }

    @Override
    public List<Product> getTopByAiScore(int limit, String platform) {
        return productMapper.selectTopByAiScore(limit, platform);
    }

    @Override
    public List<DailyStats> getTrendData(TrendQueryDTO dto) {
        return dailyStatsMapper.selectTrendByProductId(dto);
    }

    @Override
    public Map<String, Object> getDashboardData() {
        String cacheKey = "dashboard:data";

        // 尝试从Redis读缓存，失败则降级查库
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof Map) {
                //noinspection unchecked
                return (Map<String, Object>) cached;
            }
        } catch (Exception e) {
            log.warn("Redis读取缓存失败，降级查库: {}", e.getMessage());
        }

        Map<String, Object> data = new HashMap<>();
        try {
            // 总商品数
            data.put("totalProducts", productMapper.selectCount(null));
            // 今日新增商品数
            Long todayCount = productMapper.selectTodayCollectCount();
            data.put("todayCollected", todayCount != null ? todayCount : 0L);
            // 平均AI评分
            List<Product> scored = productMapper.selectList(
                new LambdaQueryWrapper<Product>()
                    .isNotNull(Product::getAiScore)
                    .select(Product::getAiScore));
            double avgScore = scored.stream()
                .mapToInt(p -> p.getAiScore() != null ? p.getAiScore() : 0)
                .average().orElse(0);
            data.put("avgAiScore", Math.round(avgScore * 10.0) / 10.0);
            // 活跃任务数
            data.put("activeTasks",
                collectTaskMapper.selectCount(new LambdaQueryWrapper<CollectTask>()
                    .eq(CollectTask::getStatus, "RUNNING")));
            // TOP5商品
            data.put("top5Products", productMapper.selectTopByAiScore(5, null));
            // 近7天每日采集趋势
            data.put("weeklyTrend", productMapper.selectDailyCollectCount(7));
            // AI决策分布
            data.put("verdictDistribution", productMapper.selectVerdictDistribution());
        } catch (Exception e) {
            log.error("查询看板数据失败", e);
            // 返回空数据，前端显示 '-' 占位
            data.putIfAbsent("totalProducts", 0);
            data.putIfAbsent("todayCollected", 0);
            data.putIfAbsent("avgAiScore", 0);
            data.putIfAbsent("activeTasks", 0);
            data.putIfAbsent("top5Products", List.of());
            data.putIfAbsent("weeklyTrend", List.of());
            data.putIfAbsent("verdictDistribution", List.of());
            return data;
        }

        // 写缓存，失败不影响返回
        try {
            redisTemplate.opsForValue().set(cacheKey, data, 5, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("Redis写入缓存失败，不影响返回: {}", e.getMessage());
        }
        return data;
    }

    @Override
    public boolean removeById(Long id) {
        return productMapper.deleteById(id) > 0;
    }

    @Override
    public ProductDetailVO getProductDetail(Long id) {
        Product product = productMapper.selectById(id);
        if (product == null) return null;
        ProductDetailVO vo = new ProductDetailVO();
        // copy fields
        vo.setId(product.getId());
        vo.setTitle(product.getTitle());
        vo.setPrice(product.getPrice());
        vo.setSales(product.getSales());
        vo.setRating(product.getRating());
        vo.setCategory(product.getCategory());
        vo.setPlatform(product.getPlatform());
        vo.setImageUrl(product.getImageUrl());
        vo.setDetailUrl(product.getDetailUrl());
        vo.setAiScore(product.getAiScore());
        vo.setAiAnalysis(product.getAiAnalysis());
        vo.setCollectTime(product.getCollectTime());
        vo.setCreateTime(product.getCreateTime());
        vo.setUpdateTime(product.getUpdateTime());

        // 近30天趋势
        TrendQueryDTO trendDto = new TrendQueryDTO();
        trendDto.setProductId(id);
        trendDto.setStartDate(LocalDate.now().minusDays(30));
        vo.setTrendStats(dailyStatsMapper.selectTrendByProductId(trendDto));

        // 排名信息
        LocalDate today = LocalDate.now();
        vo.setTodayRank(rankingHistoryMapper.selectLatestRank(id, "TODAY", today));
        vo.setWeekRank(rankingHistoryMapper.selectLatestRank(id, "WEEK", today));
        vo.setMonthRank(rankingHistoryMapper.selectLatestRank(id, "MONTH", today));
        vo.setRankingHistory(rankingHistoryMapper.selectByProductAndType(id, "TODAY", 30));
        return vo;
    }

    @Override
    @Cacheable(value = "categories", unless = "#result == null || #result.isEmpty()")
    public List<String> getCategories() {
        return productMapper.selectDistinctCategories();
    }

    @Override
    public List<CategoryTrendVO> getCategoryTrend(String category, int days) {
        return productMapper.selectCategoryTrend(category, days);
    }
}
