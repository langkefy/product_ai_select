package com.select.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.select.product.entity.RankingHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface RankingHistoryMapper extends BaseMapper<RankingHistory> {
    List<RankingHistory> selectByProductAndType(
        @Param("productId") Long productId,
        @Param("rankType") String rankType,
        @Param("days") int days);

    Integer selectLatestRank(
        @Param("productId") Long productId,
        @Param("rankType") String rankType,
        @Param("statDate") LocalDate statDate);
}

