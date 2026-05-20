package com.select.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.select.product.entity.KeywordConfig;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface KeywordConfigMapper extends BaseMapper<KeywordConfig> {
    List<KeywordConfig> selectEnabled();
}

