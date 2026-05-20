package com.select.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.select.product.dto.CollectTaskDTO;
import com.select.product.entity.CollectTask;
import com.select.product.entity.PlatformConfig;
import com.select.product.entity.Product;
import com.select.product.mapper.CollectTaskMapper;
import com.select.product.mapper.ProductMapper;
import com.select.product.service.AIService;
import com.select.product.service.CollectService;
import com.select.product.service.OpenClawService;
import com.select.product.service.PlatformConfigService;
import com.select.product.task.AsyncCollectRunner;
import cn.hutool.json.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class CollectServiceImpl implements CollectService {

    private final CollectTaskMapper collectTaskMapper;
    private final ProductMapper productMapper;
    private final AIService aiService;
    private final PlatformConfigService platformConfigService;
    private final OpenClawService api99Service;
    private final OneboundServiceImpl oneboundService;
    private final Ali1688OpenServiceImpl ali1688OpenService;
    private final AsyncCollectRunner asyncCollectRunner;

    public CollectServiceImpl(
            CollectTaskMapper collectTaskMapper,
            ProductMapper productMapper,
            AIService aiService,
            PlatformConfigService platformConfigService,
            @Qualifier("api99Service") OpenClawService api99Service,
            @Qualifier("oneboundService") OneboundServiceImpl oneboundService,
            @Qualifier("ali1688OpenService") Ali1688OpenServiceImpl ali1688OpenService,
            @Lazy AsyncCollectRunner asyncCollectRunner) {
        this.collectTaskMapper = collectTaskMapper;
        this.productMapper = productMapper;
        this.aiService = aiService;
        this.platformConfigService = platformConfigService;
        this.api99Service = api99Service;
        this.oneboundService = oneboundService;
        this.ali1688OpenService = ali1688OpenService;
        this.asyncCollectRunner = asyncCollectRunner;
    }

    /**
     * 根据任务平台 + 激活的平台配置选择对应的采集服务。
     * 路由规则：
     *  1. 任务平台为 "1688" 且存在激活的 ALI_OPEN 配置 → 使用 1688开放平台SDK
     *  2. 任务平台为 "1688" 但激活的是 OPENCLAW → 使用 onebound（onebound 支持1688）
     *  3. 任务平台为 taobao/jd/pdd 且激活 OPENCLAW → 使用 onebound
     *  4. 其余情况 → 99API
     */
    private OpenClawService resolveService(String taskPlatform) {
        PlatformConfig active = platformConfigService.getActive();

        boolean is1688Task = "1688".equalsIgnoreCase(taskPlatform);

        if (active != null && "ALI_OPEN".equalsIgnoreCase(active.getPlatformType()) && is1688Task) {
            ali1688OpenService.setConfig(active.getApiUrl(), active.getApiKey(), active.getApiSecret());
            if (active.getAccessToken() != null && !active.getAccessToken().isEmpty()) {
                ali1688OpenService.setAccessToken(active.getAccessToken());
            }
            log.info("采集路由: 任务平台=1688, 使用 1688开放平台SDK ({})", active.getName());
            return ali1688OpenService;
        }

        if (active != null && "OPENCLAW".equalsIgnoreCase(active.getPlatformType())) {
            oneboundService.setConfig(active.getApiUrl(), active.getApiKey(), active.getApiSecret());
            log.info("采集路由: 任务平台={}, 使用 OpenClaw ({})", taskPlatform, active.getName());
            return oneboundService;
        }

        if (active != null && !"ALI_OPEN".equalsIgnoreCase(active.getPlatformType())) {
            ((OpenClawServiceImpl) api99Service).setConfig(active.getApiUrl(), active.getApiKey(), null);
            log.info("采集路由: 任务平台={}, 使用 99API ({})", taskPlatform, active.getName());
            return api99Service;
        }

        // ALI_OPEN 激活但任务平台不是1688（如taobao/jd），降级到99API
        if (active != null && "ALI_OPEN".equalsIgnoreCase(active.getPlatformType()) && !is1688Task) {
            log.warn("采集路由: 当前激活平台为ALI_OPEN(仅支持1688)，但任务平台={}，降级使用99API（无API Key，可能失败）", taskPlatform);
            return api99Service;
        }

        log.warn("采集路由: 未找到激活的平台配置，默认使用99API");
        return api99Service;
    }

    /** 兼容旧调用（无任务平台参数时） */
    private OpenClawService resolveService() {
        return resolveService(null);
    }

    @Override
    @Transactional
    public CollectTask createTask(CollectTaskDTO dto) {
        CollectTask task = new CollectTask();
        task.setTaskName(dto.getTaskName());
        task.setPlatform(dto.getPlatform());
        task.setKeyword(dto.getKeyword());
        task.setMaxCount(dto.getMaxCount());
        task.setStatus("PENDING");
        task.setTotalCount(0);
        task.setSuccessCount(0);
        // 保存筛选条件
        if (Boolean.TRUE.equals(dto.getFilterDropShipping())) task.setFilterDropShipping(1);
        if (Boolean.TRUE.equals(dto.getFilterDelivery48h()))  task.setFilterDelivery48h(1);
        if (Boolean.TRUE.equals(dto.getFilterFreeShipping())) task.setFilterFreeShipping(1);
        if (Boolean.TRUE.equals(dto.getFilterDouyinSheet()))  task.setFilterDouyinSheet(1);
        collectTaskMapper.insert(task);
        log.info("创建采集任务: {}", task.getId());
        return task;
    }

    @Override
    @Transactional
    public CollectTask executeTask(Long taskId) {
        CollectTask task = collectTaskMapper.selectById(taskId);
        if (task == null) throw new RuntimeException("任务不存在: " + taskId);
        if (!"PENDING".equals(task.getStatus()) && !"FAILED".equals(task.getStatus())) {
            throw new RuntimeException("任务状态不允许执行: " + task.getStatus());
        }

        task.setStatus("RUNNING");
        task.setStartTime(LocalDateTime.now());
        task.setErrorMsg(null);
        collectTaskMapper.updateById(task);

        // 通过独立组件调用，确保 @Async 代理生效（同类内调用会绕过 Spring AOP）
        asyncCollectRunner.run(task);
        return task;
    }

    /** 供 AsyncCollectRunner 调用的采集逻辑 */
    public void doCollect(CollectTask task) {
        try {
            // 按任务的目标平台路由，确保1688任务走1688SDK，taobao任务走onebound
            OpenClawService service = resolveService(task.getPlatform());

            // 构建 filter 参数（onebound 1688 支持的过滤值）
            List<String> filterParts = new ArrayList<>();
            if (Integer.valueOf(1).equals(task.getFilterDropShipping())) filterParts.add("dropshipping");
            if (Integer.valueOf(1).equals(task.getFilterDelivery48h()))  filterParts.add("guarantee");
            if (Integer.valueOf(1).equals(task.getFilterFreeShipping()))  filterParts.add("freeshipping");
            String filterStr = String.join(",", filterParts);

            String result = service.submitCrawlTask(task.getPlatform(), task.getKeyword(), task.getMaxCount(), filterStr);

            if (result == null) {
                task.setStatus("FAILED");
                task.setErrorMsg("采集请求失败，请检查平台配置及网络连接");
                task.setEndTime(LocalDateTime.now());
                collectTaskMapper.updateById(task);
                return;
            }

            task.setOpenClawTaskId(result.length() > 400 ? result.substring(0, 400) : result);
            Object rawData = service.getTaskResult(result);
            List<Product> products = service.parseProductData(rawData);

            int success = 0;
            LocalDate today = LocalDate.now();
            for (Product p : products) {
                p.setPlatform(task.getPlatform());
                p.setCollectTime(LocalDateTime.now());
                p.setCollectDate(today);
                p.setCollectTaskId(task.getId());
                try {
                    if (StringUtils.hasText(p.getNumIid())) {
                        Product existing = productMapper.selectOne(
                            new LambdaQueryWrapper<Product>()
                                .eq(Product::getNumIid, p.getNumIid())
                                .eq(Product::getCollectDate, today)
                                .eq(Product::getPlatform, task.getPlatform())
                                .last("LIMIT 1"));
                        if (existing != null) {
                            existing.setSales(p.getSales());
                            existing.setPrice(p.getPrice());
                            existing.setAgentPrice(p.getAgentPrice());
                            existing.setCollectTime(p.getCollectTime());
                            productMapper.updateById(existing);
                            success++;
                            continue;
                        }
                    }
                    productMapper.insert(p);
                    success++;
                } catch (Exception e) {
                    log.warn("商品入库失败: {}", e.getMessage());
                }
            }

            task.setTotalCount(products.size());
            task.setSuccessCount(success);
            task.setStatus("SUCCESS");
            task.setEndTime(LocalDateTime.now());
            collectTaskMapper.updateById(task);
            log.info("采集任务完成: taskId={}, total={}, success={}", task.getId(), products.size(), success);

        } catch (Exception e) {
            log.error("采集任务执行异常: taskId={}", task.getId(), e);
            task.setStatus("FAILED");
            task.setErrorMsg(buildErrorMsg(e));
            task.setEndTime(LocalDateTime.now());
            collectTaskMapper.updateById(task);
        }
    }

    private String buildErrorMsg(Throwable e) {
        StringBuilder sb = new StringBuilder();
        Throwable current = e;
        int depth = 0;
        while (current != null && depth < 5) {
            if (depth > 0) sb.append("\n原因: ");
            String msg = current.getMessage();
            sb.append(current.getClass().getSimpleName())
              .append(": ")
              .append(msg != null ? msg : "(无描述)");
            current = current.getCause();
            depth++;
        }
        return sb.toString();
    }

    @Override
    public CollectTask syncTaskStatus(Long taskId) {
        CollectTask task = collectTaskMapper.selectById(taskId);
        if (task == null) throw new RuntimeException("任务不存在: " + taskId);
        return task;
    }

    @Override
    public Page<CollectTask> getTaskList(int page, int size) {
        Page<CollectTask> p = new Page<>(page, size);
        return collectTaskMapper.selectPage(p,
            new LambdaQueryWrapper<CollectTask>().orderByDesc(CollectTask::getCreateTime));
    }

    @Override
    public CollectTask retryTask(Long taskId) {
        CollectTask task = collectTaskMapper.selectById(taskId);
        if (task == null) throw new RuntimeException("任务不存在: " + taskId);
        task.setStatus("PENDING");
        task.setErrorMsg(null);
        task.setStartTime(null);
        task.setEndTime(null);
        collectTaskMapper.updateById(task);
        return executeTask(taskId);
    }

    @Override
    public Map<String, Object> fillMissingDetail() {
        PlatformConfig active = platformConfigService.getActive();
        if (active == null) {
            throw new RuntimeException("补全功能需要激活平台配置（OpenClaw 或 1688开放平台）");
        }

        boolean isAliOpen = "ALI_OPEN".equalsIgnoreCase(active.getPlatformType());
        boolean isOpenClaw = "OPENCLAW".equalsIgnoreCase(active.getPlatformType());

        if (!isAliOpen && !isOpenClaw) {
            throw new RuntimeException("补全功能需要激活 OpenClaw (onebound) 或 1688开放平台 (ALI_OPEN) 配置");
        }

        if (isOpenClaw) {
            oneboundService.setConfig(active.getApiUrl(), active.getApiKey(), active.getApiSecret());
        } else {
            ali1688OpenService.setConfig(active.getApiUrl(), active.getApiKey(), active.getApiSecret());
            if (active.getAccessToken() != null && !active.getAccessToken().isEmpty()) {
                ali1688OpenService.setAccessToken(active.getAccessToken());
            }
        }

        // 查询缺少发货地、且有 numIid 的商品
        List<Product> products = productMapper.selectList(
            new LambdaQueryWrapper<Product>()
                .isNotNull(Product::getNumIid)
                .ne(Product::getNumIid, "")
                .and(w -> w.isNull(Product::getLocation).or().eq(Product::getLocation, ""))
        );

        int total = products.size();
        int success = 0;
        int failed = 0;
        for (Product p : products) {
            try {
                if (isOpenClaw) {
                    String platform = p.getPlatform() != null ? p.getPlatform() : "taobao";
                    JSONObject item = oneboundService.fetchItemDetail(platform, p.getNumIid());
                    if (item == null) { failed++; continue; }
                    fillProductFromOneboundDetail(p, item);
                } else {
                    com.alibaba.product.param.AlibabaProductProductInfo info = ali1688OpenService.fetchItemDetail(p.getNumIid());
                    if (info == null) { failed++; continue; }
                    ali1688OpenService.fillProductFromDetail(p, info);
                }
                productMapper.updateById(p);
                success++;
                Thread.sleep(300);
            } catch (Exception e) {
                log.warn("[fillMissingDetail] 商品{}处理失败: {}", p.getId(), e.getMessage());
                failed++;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("success", success);
        result.put("failed", failed);
        log.info("[fillMissingDetail] 完成: total={}, success={}, failed={}", total, success, failed);
        return result;
    }

    @Override
    public Map<String, Object> fillMissingDetailByTask(Long taskId) {
        PlatformConfig active = platformConfigService.getActive();
        if (active == null) {
            throw new RuntimeException("补全功能需要激活平台配置（OpenClaw 或 1688开放平台）");
        }

        boolean isAliOpen = "ALI_OPEN".equalsIgnoreCase(active.getPlatformType());
        boolean isOpenClaw = "OPENCLAW".equalsIgnoreCase(active.getPlatformType());

        if (!isAliOpen && !isOpenClaw) {
            throw new RuntimeException("补全功能需要激活 OpenClaw (onebound) 或 1688开放平台 (ALI_OPEN) 配置");
        }

        if (isOpenClaw) {
            oneboundService.setConfig(active.getApiUrl(), active.getApiKey(), active.getApiSecret());
        } else {
            ali1688OpenService.setConfig(active.getApiUrl(), active.getApiKey(), active.getApiSecret());
            if (active.getAccessToken() != null && !active.getAccessToken().isEmpty()) {
                ali1688OpenService.setAccessToken(active.getAccessToken());
            }
        }

        CollectTask task = collectTaskMapper.selectById(taskId);
        if (task == null) throw new RuntimeException("任务不存在: " + taskId);

        task.setFillDetailStatus("RUNNING");
        task.setFillDetailCount(0);
        task.setFillDetailSuccess(0);
        task.setFillDetailFailed(0);
        collectTaskMapper.updateById(task);

        List<Product> products = productMapper.selectList(
            new LambdaQueryWrapper<Product>()
                .isNotNull(Product::getNumIid)
                .ne(Product::getNumIid, "")
                .and(w -> w.isNull(Product::getLocation).or().eq(Product::getLocation, ""))
        );

        int total = products.size();
        int success = 0;
        int failed = 0;
        for (Product p : products) {
            try {
                if (isOpenClaw) {
                    String platform = p.getPlatform() != null ? p.getPlatform() : "taobao";
                    JSONObject item = oneboundService.fetchItemDetail(platform, p.getNumIid());
                    if (item == null) { failed++; continue; }
                    fillProductFromOneboundDetail(p, item);
                } else {
                    com.alibaba.product.param.AlibabaProductProductInfo info = ali1688OpenService.fetchItemDetail(p.getNumIid());
                    if (info == null) { failed++; continue; }
                    ali1688OpenService.fillProductFromDetail(p, info);
                }
                productMapper.updateById(p);
                success++;
                Thread.sleep(300);
            } catch (Exception e) {
                log.warn("[fillMissingDetailByTask] 商品{}处理失败: {}", p.getId(), e.getMessage());
                failed++;
            }
        }

        task.setFillDetailCount(total);
        task.setFillDetailSuccess(success);
        task.setFillDetailFailed(failed);
        task.setFillDetailStatus("DONE");
        collectTaskMapper.updateById(task);

        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("success", success);
        result.put("failed", failed);
        log.info("[fillMissingDetailByTask] taskId={} 完成: total={}, success={}, failed={}", taskId, total, success, failed);
        return result;
    }

    /**
     * 从 OpenClaw (onebound) item_get 详情回填商品字段（原有逻辑提取为私有方法）
     */
    private void fillProductFromOneboundDetail(Product p, JSONObject item) {
        // 发货地
        String loc = item.getStr("location", "");
        if (org.springframework.util.StringUtils.hasText(loc)) p.setLocation(loc);

        // 平台建议零售价
        Object suggestivePrice = item.getObj("suggestive_price");
        if (suggestivePrice != null) {
            try { p.setPlatformSuggestPrice(new BigDecimal(suggestivePrice.toString())); } catch (Exception ignored) {}
        }

        // 参考物流费
        String expressFeeStr = item.getStr("express_fee", "");
        if (org.springframework.util.StringUtils.hasText(expressFeeStr)) {
            try {
                BigDecimal fee = new BigDecimal(expressFeeStr.trim());
                if (fee.compareTo(BigDecimal.ZERO) >= 0) p.setShippingFee(fee);
            } catch (Exception ignored) {}
        }

        // 代发价
        String agentPriceStr = item.getStr("agent_price", "");
        if (org.springframework.util.StringUtils.hasText(agentPriceStr) && p.getAgentPrice() == null) {
            try { p.setAgentPrice(new BigDecimal(agentPriceStr.trim())); } catch (Exception ignored) {}
        }
        if (p.getAgentPrice() == null) {
            JSONObject skusObj = item.getJSONObject("skus");
            if (skusObj != null) {
                cn.hutool.json.JSONArray skuList = skusObj.getJSONArray("sku");
                if (skuList != null) {
                    BigDecimal minSku = null;
                    for (int i = 0; i < skuList.size(); i++) {
                        JSONObject sku = skuList.getJSONObject(i);
                        String sp = sku.getStr("price", "");
                        try {
                            BigDecimal bd = new BigDecimal(sp.trim());
                            if (minSku == null || bd.compareTo(minSku) < 0) minSku = bd;
                        } catch (Exception ignored) {}
                    }
                    if (minSku != null) p.setAgentPrice(minSku);
                }
            }
        }

        // 48小时发货
        String stuffStatus = item.getStr("stuff_status", "");
        if (org.springframework.util.StringUtils.hasText(stuffStatus)) {
            p.setDeliveryIn48h(stuffStatus.contains("48") ? 1 : 0);
        }
    }

    @Override
    public Map<String, Object> clearTaskProducts(Long taskId) {
        CollectTask task = collectTaskMapper.selectById(taskId);
        if (task == null) throw new RuntimeException("任务不存在: " + taskId);

        // 按 collect_task_id 逻辑删除商品
        int deleted = productMapper.delete(
            new LambdaQueryWrapper<Product>()
                .eq(Product::getCollectTaskId, taskId)
        );

        // 重置任务计数
        task.setTotalCount(0);
        task.setSuccessCount(0);
        task.setStatus("PENDING");
        task.setStartTime(null);
        task.setEndTime(null);
        task.setErrorMsg(null);
        collectTaskMapper.updateById(task);

        Map<String, Object> result = new HashMap<>();
        result.put("deleted", deleted);
        result.put("taskId", taskId);
        log.info("[clearTaskProducts] taskId={} 已清除商品数: {}", taskId, deleted);
        return result;
    }
}
