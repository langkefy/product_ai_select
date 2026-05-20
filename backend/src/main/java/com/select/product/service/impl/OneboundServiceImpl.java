package com.select.product.service.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.select.product.entity.Product;
import com.select.product.service.OpenClawService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * OpenClaw (api-gw.onebound.cn) 采集服务实现
 * 认证参数: key + secret
 * 成功判断: success=1 且 error_code 为空/0
 */
@Slf4j
@Service("oneboundService")
public class OneboundServiceImpl implements OpenClawService {

    /** 在运行时由 CollectServiceImpl 动态注入平台配置 */
    private String apiUrl;
    private String apiKey;
    private String apiSecret;

    public void setConfig(String apiUrl, String apiKey, String apiSecret) {
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
    }

    @Override
    public String submitCrawlTask(String platform, String keyword, int maxCount, String filterStr) {
        String url = apiUrl + "/" + platform + "/item_search/";
        int pageSize = Math.min(40, maxCount);
        int pages = (int) Math.ceil((double) maxCount / pageSize);

        // 构建 filter 参数：onebound 1688 支持 dropshipping(一件代发)、guarantee 等
        String filter = filterStr != null ? filterStr : "";

        log.info("==== OpenClaw 采集请求 ====");
        log.info("  平台: {}, 关键词: {}, 数量: {}, filter={}", platform, keyword, maxCount, filter);

        StringBuilder allItems = new StringBuilder();
        allItems.append("{\"items\":{\"item\":[");
        boolean first = true;

        for (int page = 1; page <= pages; page++) {
            try {
                HttpResponse response = HttpRequest.get(url)
                        .form("key",           apiKey)
                        .form("secret",        apiSecret)
                        .form("q",             keyword)
                        .form("page",          page)
                        .form("page_size",     pageSize)
                        .form("sort",          "_sale")
                        .form("start_price",   0)
                        .form("end_price",     0)
                        .form("cat",           0)
                        .form("discount_only", "")
                        .form("seller_info",   "no")
                        .form("nick",          "")
                        .form("ppath",         "")
                        .form("imgid",         "")
                        .form("filter",        filter)
                        .form("cache",         "no")
                        .form("lang",          "zh-CN")
                        .timeout(30000)
                        .execute();

                log.info("  [第{}/{}页] HTTP状态码: {}", page, pages, response.getStatus());

                if (!response.isOk()) {
                    throw new RuntimeException("OpenClaw HTTP请求失败，状态码: " + response.getStatus());
                }

                String body = response.body();
                log.info("  [第{}页] 响应: {}", page,
                        body.length() > 800 ? body.substring(0, 800) + "\n  ...(共" + body.length() + "字符)" : body);

                String trimmed = body.trim();
                if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
                    throw new RuntimeException("API返回了非JSON内容，请检查【API地址】配置。响应前150字符: "
                            + body.substring(0, Math.min(150, body.length())));
                }

                JSONObject result = JSONUtil.parseObj(body);
                int success = result.getInt("success", 1);
                if (success == 0) {
                    String errorCode = result.getStr("error_code", "");
                    String reason = result.getStr("reason",
                            result.getStr("error", result.getStr("message", "未知错误")));
                    throw new RuntimeException(String.format("OpenClaw API返回错误: error_code=%s, reason=%s", errorCode, reason));
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
                log.error("  [第{}页] 异常: {}", page, e.getMessage(), e);
                throw new RuntimeException("第" + page + "页采集异常: " + e.getMessage(), e);
            }
        }

        allItems.append("]}}");
        String taskId = platform + "_" + keyword + "_" + System.currentTimeMillis();
        log.info("  ✓ 采集完成, taskId={}", taskId);
        log.info("==========================");
        return taskId + "##" + allItems;
    }

    private JSONArray extractItemArray(JSONObject result) {
        // items.item (OpenClaw 标准结构)
        JSONObject items = result.getJSONObject("items");
        if (items != null) {
            JSONArray arr = items.getJSONArray("item");
            if (arr != null && !arr.isEmpty()) return arr;
        }
        // 兼容 data.items.item
        JSONObject data = result.getJSONObject("data");
        if (data != null) {
            JSONObject dataItems = data.getJSONObject("items");
            if (dataItems != null) {
                JSONArray arr = dataItems.getJSONArray("item");
                if (arr != null && !arr.isEmpty()) return arr;
            }
            JSONArray arr = data.getJSONArray("item");
            if (arr != null && !arr.isEmpty()) return arr;
        }
        return null;
    }

    @Override
    public Object getTaskResult(String taskId) {
        if (taskId != null && taskId.contains("##")) {
            return taskId.split("##", 2)[1];
        }
        return null;
    }

    /**
     * 调用 item_get 接口获取商品详情，补全发货地、建议售价等字段
     * @param platform 平台 (taobao/jd/pdd/1688)
     * @param numIid   商品ID
     * @return item JSON对象，失败返回null
     */
    public JSONObject fetchItemDetail(String platform, String numIid) {
        if (apiUrl == null || apiKey == null || numIid == null || numIid.isEmpty()) return null;
        String url = apiUrl + "/" + platform + "/item_get/";
        try {
            log.info("[item_get] platform={}, numIid={}", platform, numIid);
            HttpResponse response = HttpRequest.get(url)
                    .form("key", apiKey)
                    .form("secret", apiSecret)
                    .form("num_iid", numIid)
                    .form("is_promotion", "1")
                    .form("cache", "no")
                    .form("lang", "zh-CN")
                    .timeout(20000)
                    .execute();
            if (!response.isOk()) {
                log.warn("[item_get] HTTP error: {}", response.getStatus());
                return null;
            }
            String body = response.body();
            JSONObject result = JSONUtil.parseObj(body);
            int success = result.getInt("success", 1);
            if (success == 0) {
                log.warn("[item_get] API error: {}", result.getStr("reason", ""));
                return null;
            }
            return result.getJSONObject("item");
        } catch (Exception e) {
            log.error("[item_get] 请求异常: numIid={}, err={}", numIid, e.getMessage());
            return null;
        }
    }

    @Override
    public List<Product> parseProductData(Object rawData) {
        List<Product> products = new ArrayList<>();
        if (rawData == null) return products;
        try {
            JSONObject json = JSONUtil.parseObj(rawData.toString());
            JSONObject items = json.getJSONObject("items");
            if (items == null) return products;
            JSONArray itemList = items.getJSONArray("item");
            if (itemList == null) return products;

            LocalDate today = LocalDate.now();
            for (int i = 0; i < itemList.size(); i++) {
                JSONObject item = itemList.getJSONObject(i);
                Product p = new Product();

                p.setNumIid(item.getStr("num_iid", item.getStr("item_id", "")));
                p.setTitle(item.getStr("title", item.getStr("subject", "")));

                String priceStr = item.getStr("price", item.getStr("zk_final_price", "0"));
                if (priceStr != null && priceStr.contains("-")) priceStr = priceStr.split("-")[0];
                try { p.setPrice(new BigDecimal(priceStr.trim())); } catch (Exception e) { p.setPrice(BigDecimal.ZERO); }

                String agentPriceStr = item.getStr("agent_price", "");
                if (!agentPriceStr.isEmpty()) {
                    try { p.setAgentPrice(new BigDecimal(agentPriceStr.trim())); } catch (Exception ignored) {}
                }

                p.setMinNum(item.getInt("min_order_quantity", item.getInt("min_num", 1)));
                p.setLocation(item.getStr("area", item.getStr("location", "")));
                p.setSales(item.getLong("sales", item.getLong("volume", 0L)));

                String scoreStr = item.getStr("score", "5.0");
                try { p.setRating(new BigDecimal(scoreStr.trim())); } catch (Exception e) { p.setRating(new BigDecimal("5.0")); }

                p.setImageUrl(item.getStr("pic_url", item.getStr("pict_url", "")));
                p.setDetailUrl(item.getStr("detail_url", item.getStr("item_url", "")));
                p.setCollectDate(today);
                p.setCollectTime(LocalDateTime.now());

                if (p.getTitle() != null && !p.getTitle().isEmpty()) {
                    products.add(p);
                }
            }
        } catch (Exception e) {
            log.error("解析 OpenClaw 商品数据失败", e);
        }
        return products;
    }
}
