package com.select.product.service;

import com.select.product.dto.KeywordConfigDTO;
import com.select.product.entity.KeywordConfig;
import java.util.List;

public interface KeywordConfigService {
    List<KeywordConfig> listAll();
    List<KeywordConfig> listEnabled();
    KeywordConfig create(KeywordConfigDTO dto);
    KeywordConfig update(Long id, KeywordConfigDTO dto);
    boolean delete(Long id);
    void triggerCollect(Long id);
}

