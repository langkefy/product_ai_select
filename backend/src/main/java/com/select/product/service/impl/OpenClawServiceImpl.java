package com.select.product.service.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.select.product.config.OpenClawConfig;
import com.select.product.entity.Product;
import com.select.product.service.OpenClawService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service("api99Service")
@RequiredArgsConstructor
public class OpenClawServiceImpl implements OpenClawService {

    private final OpenClawConfig config;

    // 动态覆盖配置（由 CollectServiceImpl 注入平台配置时使用）
    private volatile String dynamicUrl;
    private volatile String dynamicApiKey;

    public void setConfig(String apiUrl, String apiKey, String apiSecret) {
        this.dynamicUrl = apiUrl;
        this.dynamicApiKey = apiKey;
    }

    @Override
    public String submitCrawlTask(String platform, String keyword, int maxCount, String filterStr) {
        String baseUrl = (dynamicUrl != null && !dynamicUrl.isEmpty()) ? dynamicUrl : config.getUrl();
        String apiKey  = (dynamicApiKey != null && !dynamicApiKey.isEmpty()) ? dynamicApiKey : config.getApiKey();
        String url = baseUrl + "/" + platform + "/search";
        int pageSize = Math.min(40, maxCount);
        int pages = (int) Math.ceil((double) maxCount / pageSize);

        log.info("==== 99API 采集请求 ====");
        log.info("  平台: {}, 关键词: {}, 数量: {} (每页{}条, 共{}页)", platform, keyword, maxCount, pageSize, pages);
        log.info("  请求地址: {}", url);
        log.info("  认证: api_key={}", apiKey);

        StringBuilder allItems = new StringBuilder();
        allItems.append("{\"items\":{\"item\":[");
        boolean first = true;

        for (int page = 1; page <= pages; page++) {
            try {
                log.info("  [第{}/{}页] GET {}?api_key={}&q={}&sort=_sale&page={}&pagesize={}",
                        page, pages, url, apiKey, keyword, page, pageSize);

                HttpResponse response = HttpRequest.get(url)
                        .form("api_key", apiKey)
                        .form("q", keyword)
                        .form("sort", "_sale")
                        .form("page", page)
                        .form("pagesize", pageSize)
                        .timeout(30000)
                        .execute();

                log.info("  [第{}页] HTTP状态码: {}", page, response.getStatus());

                if (!response.isOk()) {
                    throw new RuntimeException("99api HTTP请求失败，状态码: " + response.getStatus());
                }

                String body = response.body();
                log.info("  [第{}页] 响应: {}", page,
                        body.length() > 800 ? body.substring(0, 800) + "\n  ...(共" + body.length() + "字符)" : body);

                // 防止 URL 配置错误时返回 HTML
                String trimmed = body.trim();
                if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
                    throw new RuntimeException("API返回了非JSON内容，请检查【API地址】配置。响应前150字符: "
                            + body.substring(0, Math.min(150, body.length())));
                }

                JSONObject result = JSONUtil.parseObj(body);

                String codeStr = result.getStr("code", "200");
                int code;
                try { code = Integer.parseInt(codeStr.trim()); } catch (Exception ex) { code = 200; }

                String msg = result.getStr("msg", result.getStr("message", result.getStr("reason", "未知错误")));

                if (code != 200 && code != 0 && code != 1) {
                    String errDetail = String.format("99api返回错误: code=%d, msg=%s", code, msg);
                    log.error("  ✗ {}", errDetail);
                    throw new RuntimeException(errDetail);
                }

                JSONArray itemArr = extractItemArray(result);
                log.info("  [第{}页] 解析商品数: {}", page, itemArr == null ? 0 : itemArr.size());

                if (itemArr != null && !itemArr.isEmpty()) {
                    for (int i = 0; i < itemArr.size(); i++) {
                        if (!first) allItems.append(",");
                        allItems.append(itemArr.get(i).toString());
                        first = false;
                    }
                }

                if (page < pages) Thread.sleep(500);

            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                log.warn("  [第{}页] 异常，跳过: {}", page, e.getMessage());
            }
        }

        allItems.append("]}}");
        String taskId = platform + "_" + keyword + "_" + System.currentTimeMillis();
        log.info("  ✓ 采集完成, 有商品={}, taskId={}", !first, taskId);
        log.info("========================");
        return taskId + "##" + allItems;
    }

    /**
     * 从99api响应中提取商品数组，兼容多种响应结构:
     * - data.items.item
     * - data.item
     * - data (array)
     * - items.item
     * - result.item
     */
    private JSONArray extractItemArray(JSONObject result) {
        // 尝试 data 层
        JSONObject data = result.getJSONObject("data");
        if (data != null) {
            // data.items.item
            JSONObject dataItems = data.getJSONObject("items");
            if (dataItems != null) {
                JSONArray arr = dataItems.getJSONArray("item");
                if (arr != null && !arr.isEmpty()) return arr;
            }
            // data.item
            JSONArray arr = data.getJSONArray("item");
            if (arr != null && !arr.isEmpty()) return arr;
            // data.list
            arr = data.getJSONArray("list");
            if (arr != null && !arr.isEmpty()) return arr;
        }
        // 兼容旧格式 items.item
        JSONObject items = result.getJSONObject("items");
        if (items != null) {
            JSONArray arr = items.getJSONArray("item");
            if (arr != null && !arr.isEmpty()) return arr;
        }
        // 兼容 result.item
        JSONObject res = result.getJSONObject("result");
        if (res != null) {
            JSONArray arr = res.getJSONArray("item");
            if (arr != null && !arr.isEmpty()) return arr;
        }
        return null;
    }

    @Override
    public Object getTaskResult(String openClawTaskId) {
        if (openClawTaskId != null && openClawTaskId.contains("##")) {
            return openClawTaskId.split("##", 2)[1];
        }
        return null;
    }

    @Override
    public List<Product> parseProductData(Object rawData) {
        List<Product> products = new ArrayList<>();
        if (rawData == null) return products;
        try {
            JSONObject json = JSONUtil.parseObj(rawData.toString());
            // 内部统一存储为 items.item 格式
            JSONObject items = json.getJSONObject("items");
            if (items == null) items = json.getJSONObject("result");
            if (items == null) return products;
            JSONArray itemList = items.getJSONArray("item");
            if (itemList == null) return products;

            LocalDate today = LocalDate.now();
            for (int i = 0; i < itemList.size(); i++) {
                JSONObject item = itemList.getJSONObject(i);
                Product p = new Product();

                // 商品ID — 99api 常用 num_iid / itemId / item_id
                p.setNumIid(item.getStr("num_iid", item.getStr("itemId",
                        item.getStr("item_id", item.getStr("id", "")))));

                // 标题
                p.setTitle(item.getStr("title", item.getStr("subject", item.getStr("name", ""))));

                // 价格 — 处理区间格式 "10.00-20.00" 取最小值
                String priceStr = item.getStr("price", item.getStr("zk_final_price",
                        item.getStr("origPrice", item.getStr("price_range", "0"))));
                if (priceStr != null && priceStr.contains("-")) priceStr = priceStr.split("-")[0];
                try { p.setPrice(new BigDecimal(priceStr.trim())); } catch (Exception e) { p.setPrice(BigDecimal.ZERO); }

                // 代发价
                String agentPriceStr = item.getStr("agent_price", item.getStr("agentPrice", ""));
                if (!agentPriceStr.isEmpty()) {
                    try { p.setAgentPrice(new BigDecimal(agentPriceStr.trim())); } catch (Exception ignored) {}
                }

                // 起批量
                p.setMinNum(item.getInt("min_order_quantity", item.getInt("min_num",
                        item.getInt("minNum", item.getInt("quantity", 1)))));

                // 发货地
                p.setLocation(item.getStr("area", item.getStr("location",
                        item.getStr("item_location", item.getStr("prov", "")))));

                // 销量
                p.setSales(item.getLong("sales", item.getLong("volume",
                        item.getLong("monthly_sold", item.getLong("sold_out", 0L)))));

                // 评分
                String scoreStr = item.getStr("score", item.getStr("seller_credit_score", "5.0"));
                try { p.setRating(new BigDecimal(scoreStr.trim())); } catch (Exception e) { p.setRating(new BigDecimal("5.0")); }

                // 图片和链接
                p.setImageUrl(item.getStr("pic_url", item.getStr("pict_url",
                        item.getStr("main_pic", item.getStr("img_url", "")))));
                p.setDetailUrl(item.getStr("detail_url", item.getStr("item_url",
                        item.getStr("url", item.getStr("product_url", "")))));

                // 采集信息
                p.setCollectDate(today);
                p.setCollectTime(LocalDateTime.now());

                if (p.getTitle() != null && !p.getTitle().isEmpty()) {
                    products.add(p);
                }
            }
        } catch (Exception e) {
            log.error("解析99api商品数据失败", e);
        }
        return products;
    }
}
