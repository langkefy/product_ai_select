package com.select.product.service;

import com.select.product.entity.PlatformConfig;
import java.util.List;

public interface PlatformConfigService {
    List<PlatformConfig> listAll();
    PlatformConfig getById(Long id);
    PlatformConfig getActive();
    PlatformConfig save(PlatformConfig config);
    PlatformConfig update(Long id, PlatformConfig config);
    void activate(Long id);
    void delete(Long id);
}

