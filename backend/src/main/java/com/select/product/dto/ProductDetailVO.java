package com.select.product.dto;

import com.select.product.entity.DailyStats;
import com.select.product.entity.Product;
import com.select.product.entity.RankingHistory;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class ProductDetailVO extends Product {
    /** 近30天每日统计 */
    private List<DailyStats> trendStats;
    /** 今日排名 */
    private Integer todayRank;
    /** 本周排名 */
    private Integer weekRank;
    /** 本月排名 */
    private Integer monthRank;
    /** 排名历史 */
    private List<RankingHistory> rankingHistory;
}

