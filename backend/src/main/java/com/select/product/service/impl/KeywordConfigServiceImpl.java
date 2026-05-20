package com.select.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.select.product.dto.CollectTaskDTO;
import com.select.product.dto.KeywordConfigDTO;
import com.select.product.entity.KeywordConfig;
import com.select.product.mapper.KeywordConfigMapper;
import com.select.product.service.CollectService;
import com.select.product.service.KeywordConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeywordConfigServiceImpl implements KeywordConfigService {

    private final KeywordConfigMapper keywordConfigMapper;
    private final CollectService collectService;

    @Override
    public List<KeywordConfig> listAll() {
        return keywordConfigMapper.selectList(
            new LambdaQueryWrapper<KeywordConfig>().orderByDesc(KeywordConfig::getPriority));
    }

    @Override
    public List<KeywordConfig> listEnabled() {
        return keywordConfigMapper.selectEnabled();
    }

    @Override
    public KeywordConfig create(KeywordConfigDTO dto) {
        KeywordConfig config = new KeywordConfig();
        copyDto(dto, config);
        keywordConfigMapper.insert(config);
        return config;
    }

    @Override
    public KeywordConfig update(Long id, KeywordConfigDTO dto) {
        KeywordConfig config = keywordConfigMapper.selectById(id);
        if (config == null) throw new RuntimeException("关键词配置不存在: " + id);
        copyDto(dto, config);
        keywordConfigMapper.updateById(config);
        return config;
    }

    @Override
    public boolean delete(Long id) {
        return keywordConfigMapper.deleteById(id) > 0;
    }

    @Override
    public void triggerCollect(Long id) {
        KeywordConfig config = keywordConfigMapper.selectById(id);
        if (config == null) throw new RuntimeException("关键词配置不存在: " + id);
        CollectTaskDTO dto = new CollectTaskDTO();
        dto.setTaskName("手动触发-" + config.getKeyword());
        dto.setPlatform(config.getPlatform());
        dto.setKeyword(config.getKeyword());
        dto.setMaxCount(config.getMaxCount() != null ? config.getMaxCount() : 50);
        var task = collectService.createTask(dto);
        collectService.executeTask(task.getId());
        log.info("手动触发关键词采集: keyword={}", config.getKeyword());
    }

    private void copyDto(KeywordConfigDTO dto, KeywordConfig config) {
        if (dto.getKeyword() != null) config.setKeyword(dto.getKeyword());
        if (dto.getPlatform() != null) config.setPlatform(dto.getPlatform());
        if (dto.getCategory() != null) config.setCategory(dto.getCategory());
        if (dto.getPriority() != null) config.setPriority(dto.getPriority());
        if (dto.getEnabled() != null) config.setEnabled(dto.getEnabled());
        if (dto.getCronExpr() != null) config.setCronExpr(dto.getCronExpr());
        if (dto.getMaxCount() != null) config.setMaxCount(dto.getMaxCount());
    }
}

