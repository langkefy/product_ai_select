package com.select.product.service.impl;

import com.select.product.dto.ProductQueryDTO;
import com.select.product.entity.Product;
import com.select.product.entity.RankingHistory;
import com.select.product.mapper.ProductMapper;
import com.select.product.mapper.RankingHistoryMapper;
import com.select.product.service.RankingHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RankingHistoryServiceImpl implements RankingHistoryService {

    private final RankingHistoryMapper rankingHistoryMapper;
    private final ProductMapper productMapper;

    @Override
    public void recordSnapshot(String rankType) {
        try {
            // 查询当前 TOP 100
            ProductQueryDTO dto = new ProductQueryDTO();
            dto.setSortField("aiScore");
            dto.setSortOrder("desc");
            dto.setSize(100);
            List<Product> top = productMapper.selectTopByAiScore(100, null);

            LocalDate today = LocalDate.now();
            for (int i = 0; i < top.size(); i++) {
                Product p = top.get(i);
                // 避免重复插入
                Integer existing = rankingHistoryMapper.selectLatestRank(p.getId(), rankType, today);
                if (existing != null) continue;

                RankingHistory history = new RankingHistory();
                history.setProductId(p.getId());
                history.setRankType(rankType);
                history.setRankPosition(i + 1);
                history.setScore(p.getAiScore());
                history.setSales(p.getSales());
                history.setStatDate(today);
                rankingHistoryMapper.insert(history);
            }
            log.info("排名快照已记录: rankType={}, count={}", rankType, top.size());
        } catch (Exception e) {
            log.error("记录排名快照失败: rankType={}", rankType, e);
        }
    }

    @Override
    public List<RankingHistory> getHistory(Long productId, String rankType, int days) {
        return rankingHistoryMapper.selectByProductAndType(productId, rankType, days);
    }
}

