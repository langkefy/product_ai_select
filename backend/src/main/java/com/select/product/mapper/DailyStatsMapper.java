package com.select.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.select.product.dto.TrendQueryDTO;
import com.select.product.entity.DailyStats;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface DailyStatsMapper extends BaseMapper<DailyStats> {

    List<DailyStats> selectTrendByProductId(@Param("dto") TrendQueryDTO dto);

    int batchInsert(@Param("list") List<DailyStats> list);
}

