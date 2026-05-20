package com.select.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.select.product.entity.PlatformConfig;
import com.select.product.mapper.PlatformConfigMapper;
import com.select.product.service.PlatformConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlatformConfigServiceImpl implements PlatformConfigService {

    private final PlatformConfigMapper mapper;

    @Override
    public List<PlatformConfig> listAll() {
        return mapper.selectList(new LambdaQueryWrapper<PlatformConfig>()
                .orderByDesc(PlatformConfig::getIsActive)
                .orderByAsc(PlatformConfig::getId));
    }

    @Override
    public PlatformConfig getById(Long id) {
        return mapper.selectById(id);
    }

    @Override
    public PlatformConfig getActive() {
        return mapper.selectOne(new LambdaQueryWrapper<PlatformConfig>()
                .eq(PlatformConfig::getIsActive, 1)
                .last("LIMIT 1"));
    }

    @Override
    public PlatformConfig save(PlatformConfig config) {
        config.setIsActive(config.getIsActive() == null ? 0 : config.getIsActive());
        mapper.insert(config);
        if (Integer.valueOf(1).equals(config.getIsActive())) {
            deactivateOthers(config.getId());
        }
        return config;
    }

    @Override
    @Transactional
    public PlatformConfig update(Long id, PlatformConfig config) {
        config.setId(id);
        mapper.updateById(config);
        if (Integer.valueOf(1).equals(config.getIsActive())) {
            deactivateOthers(id);
        }
        return mapper.selectById(id);
    }

    @Override
    @Transactional
    public void activate(Long id) {
        // 将所有置为非激活
        mapper.update(null, new LambdaUpdateWrapper<PlatformConfig>()
                .set(PlatformConfig::getIsActive, 0));
        // 激活指定
        PlatformConfig config = new PlatformConfig();
        config.setId(id);
        config.setIsActive(1);
        mapper.updateById(config);
        log.info("已激活采集平台配置, id={}", id);
    }

    @Override
    public void delete(Long id) {
        mapper.deleteById(id);
    }

    private void deactivateOthers(Long excludeId) {
        mapper.update(null, new LambdaUpdateWrapper<PlatformConfig>()
                .set(PlatformConfig::getIsActive, 0)
                .ne(PlatformConfig::getId, excludeId));
    }
}

