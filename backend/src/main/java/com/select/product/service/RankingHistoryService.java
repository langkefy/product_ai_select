package com.select.product.service;

import com.select.product.entity.RankingHistory;
import java.util.List;

public interface RankingHistoryService {
    void recordSnapshot(String rankType);
    List<RankingHistory> getHistory(Long productId, String rankType, int days);
}

