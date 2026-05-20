package com.select.product.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.select.product.dto.CollectTaskDTO;
import com.select.product.entity.CollectTask;
import java.util.Map;

public interface CollectService {
    CollectTask createTask(CollectTaskDTO dto);
    CollectTask executeTask(Long taskId);
    CollectTask syncTaskStatus(Long taskId);
    Page<CollectTask> getTaskList(int page, int size);
    CollectTask retryTask(Long taskId);
    /** 调用item_get补全缺少发货地的商品详情（发货地、建议售价等） */
    Map<String, Object> fillMissingDetail();
    /** 按任务ID补全该任务关联商品的详情，并记录补全进度到任务表 */
    Map<String, Object> fillMissingDetailByTask(Long taskId);
    /** 清除某个采集任务的所有商品数据（逻辑删除） */
    Map<String, Object> clearTaskProducts(Long taskId);
}

