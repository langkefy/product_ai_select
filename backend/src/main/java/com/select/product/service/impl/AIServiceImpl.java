package com.select.product.service.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.select.product.config.DeepSeekConfig;
import com.select.product.entity.Product;
import com.select.product.mapper.ProductMapper;
import com.select.product.service.AIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIServiceImpl implements AIService {

    private final DeepSeekConfig deepSeekConfig;
    private final ProductMapper productMapper;
    private final RestTemplate restTemplate;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String CACHE_PREFIX = "ai:analysis:";
    private static final long CACHE_TTL_HOURS = 168; // 7天

    @Override
    public Product analyzeProduct(Long productId) {
        Product product = productMapper.selectById(productId);
        if (product == null) return null;

        // 检查缓存（Redis不可用时忽略）
        String cacheKey = CACHE_PREFIX + productId;
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null && product.getAiScore() != null) {
                log.info("命中AI分析缓存, productId={}", productId);
                return product;
            }
        } catch (Exception e) {
            log.warn("Redis读缓存失败，继续调用AI: {}", e.getMessage());
        }

        String prompt = buildPrompt(product);
        String analysisResult = callDeepSeek(prompt);

        // AI API不可用时，使用本地规则引擎兜底
        if (analysisResult == null) {
            log.info("AI API不可用，使用规则引擎兜底分析, productId={}", productId);
            analysisResult = ruleBasedAnalysis(product);
        }

        if (analysisResult != null) {
            parseAndSaveAIResult(product, analysisResult);
            productMapper.updateById(product);
            try {
                redisTemplate.opsForValue().set(cacheKey, analysisResult, CACHE_TTL_HOURS, TimeUnit.HOURS);
            } catch (Exception e) {
                log.warn("Redis写缓存失败: {}", e.getMessage());
            }
            log.info("AI分析完成, productId={}, score={}, verdict={}", productId, product.getAiScore(), product.getVerdict());
        }
        return product;
    }

    @Override
    public List<Product> analyzeAllUnanalyzed() {
        // 查询所有未分析的商品（aiScore为空）
        List<Product> unanalyzed = productMapper.selectList(
            new LambdaQueryWrapper<Product>().isNull(Product::getAiScore)
        );
        log.info("待分析商品数量: {}", unanalyzed.size());
        List<Long> ids = unanalyzed.stream().map(Product::getId).collect(Collectors.toList());
        List<Product> results = batchAnalyze(ids);
        // 按决策排序：上架 > 测试 > 放弃 > 其他
        Map<String, Integer> order = new HashMap<>();
        order.put("上架", 0);
        order.put("测试", 1);
        order.put("放弃", 2);
        results.sort(Comparator.comparingInt(p -> order.getOrDefault(p.getVerdict(), 3)));
        return results;
    }

    @Override
    public List<Product> batchAnalyze(List<Long> productIds) {
        List<Product> results = new ArrayList<>();
        for (Long id : productIds) {
            try {
                Product p = analyzeProduct(id);
                if (p != null) results.add(p);
                Thread.sleep(500); // 避免API限流
            } catch (Exception e) {
                log.error("批量AI分析失败, productId={}", id, e);
            }
        }
        return results;
    }

    @Override
    public String generateRankingReport(String platform, String category) {
        String cacheKey = "ai:report:" + (platform == null ? "all" : platform) + ":" + (category == null ? "all" : category);
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) return cached.toString();
        } catch (Exception e) {
            log.warn("Redis读缓存失败: {}", e.getMessage());
        }

        String prompt = String.format(
            "你是选品专家。请根据以下信息生成一份选品分析报告：平台：%s，品类：%s。\n" +
            "请从市场趋势、竞争格局、选品建议三个维度进行分析，控制在500字以内。",
            platform == null ? "全平台" : platform,
            category == null ? "全品类" : category
        );
        String report = callDeepSeek(prompt);
        if (report == null) {
            report = String.format("【规则引擎报告】平台：%s，品类：%s。\n当前AI服务暂时不可用，建议关注该品类近期销量趋势、竞品定价及买家评价，结合平台流量数据综合判断选品方向。",
                platform == null ? "全平台" : platform,
                category == null ? "全品类" : category);
        }
        if (report != null) {
            try {
                redisTemplate.opsForValue().set(cacheKey, report, 2, TimeUnit.HOURS);
            } catch (Exception e) {
                log.warn("Redis写缓存失败: {}", e.getMessage());
            }
        }
        return report != null ? report : "暂无分析报告";
    }

    private String buildPrompt(Product product) {
        String shippingInfo = (product.getShippingFee() != null && product.getShippingFee().compareTo(BigDecimal.ZERO) > 0)
            ? product.getShippingFee().toString() + "元"
            : "未知（请按5-8元估算）";

        StringBuilder sb = new StringBuilder();
        sb.append("你是选品专家。分析以下商品，输出JSON格式评估。\n\n");
        sb.append("商品信息：\n");
        sb.append("- 标题：").append(product.getTitle() != null ? product.getTitle() : "未知").append("\n");
        sb.append(String.format("- 采购价：%.2f元\n", product.getPrice() != null ? product.getPrice().doubleValue() : 0));
        sb.append("- 代发价：").append(product.getAgentPrice() != null ? product.getAgentPrice().toString() + "元" : "未知").append("\n");
        sb.append("- 参考物流费：").append(shippingInfo).append("\n");
        sb.append("- 30天销量：").append(product.getSales() != null ? product.getSales() : 0).append("\n");
        sb.append("- 起批量：").append(product.getMinNum() != null ? product.getMinNum() : 1).append("\n");
        if (product.getLocation() != null && !product.getLocation().isEmpty()) {
            sb.append("- 产地/发货地：").append(product.getLocation()).append("\n");
        }
        if (product.getDeliveryIn48h() != null) {
            sb.append("- 48小时内发货：").append(product.getDeliveryIn48h() == 1 ? "支持" : "不支持").append("\n");
        }
        if (product.getFullAfterSales() != null) {
            sb.append("- 全包售后（退换货供应商承担）：").append(product.getFullAfterSales() == 1 ? "支持" : "不支持").append("\n");
        }
        if (product.getDouyinSheetSupport() != null) {
            sb.append("- 支持抖音面单：").append(product.getDouyinSheetSupport() == 1 ? "支持" : "不支持").append("\n");
        }
        if (product.getAutoSyncStock() != null) {
            sb.append("- 自动同步库存：").append(product.getAutoSyncStock() == 1 ? "支持" : "不支持").append("\n");
        }
        sb.append("\n注意：建议零售价需在【代发价 + 物流费】基础上保证合理利润空间。");
        sb.append("供应商服务能力（发货时效、售后、面单、库存同步）也是重要加分项。\n\n");
        sb.append("请输出以下JSON格式（只输出JSON，不要有其他内容）：\n");
        sb.append("{\"score\": 0-100的整数, \"verdict\": \"上架/测试/放弃\", ");
        sb.append("\"reason\": \"一句话理由(不超过50字)\", ");
        sb.append("\"suggested_price\": 建议零售价数字(含物流成本后的合理售价), ");
        sb.append("\"risk\": \"风险提示(不超过30字)\", ");
        sb.append("\"ai_title\": \"根据商品信息重新撰写的吸引买家的标题(不超过30字，突出卖点，适合抖音/电商)\"}");
        return sb.toString();
    }

    private void parseAndSaveAIResult(Product product, String text) {
        try {
            // 提取JSON块
            String clean = text.trim();
            int start = clean.indexOf('{');
            int end = clean.lastIndexOf('}');
            if (start >= 0 && end > start) {
                JSONObject json = JSONUtil.parseObj(clean.substring(start, end + 1));
                product.setAiScore(Math.min(100, Math.max(0, json.getInt("score", 60))));
                product.setVerdict(json.getStr("verdict", "测试"));
                product.setRisk(json.getStr("risk", ""));
                product.setAiAnalysis(json.getStr("reason", text));
                // 建议售价
                Object sp = json.get("suggested_price");
                if (sp != null) {
                    try { product.setSuggestedPrice(new BigDecimal(sp.toString())); } catch (Exception ignored) {}
                }
                // AI新标题
                String aiTitle = json.getStr("ai_title", "");
                if (StringUtils.hasText(aiTitle)) {
                    product.setAiTitle(aiTitle);
                }
            } else {
                product.setAiScore(60);
                product.setAiAnalysis(text);
            }
        } catch (Exception e) {
            log.warn("解析AI结果失败，使用默认值: {}", e.getMessage());
            product.setAiScore(60);
            product.setAiAnalysis(text);
        }
    }

    private String callDeepSeek(String prompt) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(deepSeekConfig.getKey());

            JSONObject requestBody = new JSONObject();
            requestBody.set("model", deepSeekConfig.getModel());
            requestBody.set("temperature", 0.3);
            requestBody.set("max_tokens", 800);
            JSONArray messages = new JSONArray();
            JSONObject sysMsg = new JSONObject();
            sysMsg.set("role", "system");
            sysMsg.set("content", "你是专业的电商选品分析师");
            messages.add(sysMsg);
            JSONObject userMsg = new JSONObject();
            userMsg.set("role", "user");
            userMsg.set("content", prompt);
            messages.add(userMsg);
            requestBody.set("messages", messages);

            HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);
            ResponseEntity<String> response = restTemplate.exchange(
                deepSeekConfig.getUrl(), HttpMethod.POST, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                JSONObject result = JSONUtil.parseObj(response.getBody());
                JSONArray choices = result.getJSONArray("choices");
                if (choices != null && !choices.isEmpty()) {
                    return choices.getJSONObject(0)
                        .getJSONObject("message")
                        .getStr("content");
                }
            }
        } catch (Exception e) {
            log.error("调用DeepSeek API失败: {}，启用本地规则引擎兜底", e.getMessage());
        }
        return null;
    }

    /**
     * 本地规则引擎兜底分析（AI API不可用时使用）
     * 根据商品销量、价格、起批量等指标进行评估
     */
    private String ruleBasedAnalysis(Product product) {
        try {
            int score = 50; // 基础分
            StringBuilder reason = new StringBuilder();
            String verdict;
            String risk = "";

            // 销量评分 (最高+30)
            long sales = product.getSales() != null ? product.getSales() : 0L;
            if (sales >= 500) {
                score += 30;
                reason.append("销量优秀;");
            } else if (sales >= 100) {
                score += 20;
                reason.append("销量良好;");
            } else if (sales >= 30) {
                score += 10;
                reason.append("销量一般;");
            } else {
                score -= 10;
                reason.append("销量偏低;");
                risk = "销量偏低，市场验证不足";
            }

            // 价格评分 (最高+15)
            double price = product.getPrice() != null ? product.getPrice().doubleValue() : 0;
            if (price > 0 && price <= 100) {
                score += 15;
                reason.append("客单价适中;");
            } else if (price > 100 && price <= 500) {
                score += 8;
                reason.append("客单价较高;");
            } else if (price > 500) {
                score += 3;
                reason.append("客单价高，受众较窄;");
                risk = risk.isEmpty() ? "高客单价，转化率偏低" : risk;
            }

            // 起批量评分 (最高+10)
            long minNum = product.getMinNum() != null ? product.getMinNum() : 1L;
            if (minNum <= 1) {
                score += 10;
            } else if (minNum <= 5) {
                score += 5;
            } else {
                score -= 5;
                risk = risk.isEmpty() ? "起批量较高，资金占用大" : risk;
            }

            // 代发价合理性 (最高+10)
            if (product.getAgentPrice() != null && product.getPrice() != null) {
                double shippingCost = product.getShippingFee() != null ? product.getShippingFee().doubleValue() : 6.0;
                double totalCost = product.getAgentPrice().doubleValue() + shippingCost;
                double marginRate = price > 0 ? (price - totalCost) / price : 0;
                if (marginRate >= 0.3) {
                    score += 10;
                    reason.append("利润空间充足;");
                } else if (marginRate >= 0.15) {
                    score += 5;
                    reason.append("利润空间一般;");
                } else {
                    score -= 5;
                    reason.append("含物流后利润偏低;");
                    risk = risk.isEmpty() ? "含物流成本后利润率偏低，需谨慎评估" : risk;
                }
            }

            score = Math.min(100, Math.max(0, score));

            // 供应商服务能力加分（每项+3，最高+12）
            int serviceBonus = 0;
            if (Integer.valueOf(1).equals(product.getDeliveryIn48h())) serviceBonus += 3;
            if (Integer.valueOf(1).equals(product.getFullAfterSales())) serviceBonus += 3;
            if (Integer.valueOf(1).equals(product.getDouyinSheetSupport())) serviceBonus += 3;
            if (Integer.valueOf(1).equals(product.getAutoSyncStock())) serviceBonus += 3;
            if (serviceBonus > 0) reason.append("供应商服务能力好;");
            score = Math.min(100, score + serviceBonus);

            if (score >= 70) {
                verdict = "上架";
            } else if (score >= 45) {
                verdict = "测试";
            } else {
                verdict = "放弃";
            }

            if (risk.isEmpty()) risk = "暂无明显风险";
            String reasonStr = reason.length() > 0
                ? reason.toString().replaceAll(";$", "").replace(";", "，")
                : "综合评估";

            // 建议售价：(代发价 + 物流费) * 1.5，保证覆盖成本后有利润
            double shippingCost = product.getShippingFee() != null ? product.getShippingFee().doubleValue() : 6.0;
            double baseCost = (product.getAgentPrice() != null ? product.getAgentPrice().doubleValue() : price) + shippingCost;
            double suggestedPrice = baseCost * 1.5;

            // 规则引擎生成简单新标题（截取原标题前20字 + 卖点后缀）
            String origTitle = product.getTitle() != null ? product.getTitle() : "";
            String shortTitle = origTitle.length() > 20 ? origTitle.substring(0, 20) : origTitle;
            String titleSuffix = verdict.equals("上架") ? "【爆款推荐】" : verdict.equals("测试") ? "【性价比优选】" : "";
            String generatedAiTitle = shortTitle + titleSuffix;

            JSONObject json = new JSONObject();
            json.set("score", score);
            json.set("verdict", verdict);
            json.set("reason", reasonStr + "（规则引擎评估）");
            json.set("suggested_price", Math.round(suggestedPrice * 10.0) / 10.0);
            json.set("risk", risk);
            json.set("ai_title", generatedAiTitle);
            return json.toString();
        } catch (Exception e) {
            log.warn("规则引擎分析失败: {}", e.getMessage());
            return "{\"score\":60,\"verdict\":\"测试\",\"reason\":\"系统自动评估\",\"suggested_price\":0,\"risk\":\"请人工复核\"}";
        }
    }

    @Override
    public List<Map<String, String>> recommendKeywords(String category, String customInput) {
        // 获取当前季节
        int month = LocalDate.now().getMonthValue();
        String season;
        if (month >= 3 && month <= 5) season = "春季";
        else if (month >= 6 && month <= 8) season = "夏季";
        else if (month >= 9 && month <= 11) season = "秋季";
        else season = "冬季";
        String dateStr = LocalDate.now().toString();

        StringBuilder prompt = new StringBuilder();
        prompt.append("你是电商选品专家。当前日期：").append(dateStr).append("，季节：").append(season).append("。\n");
        if (StringUtils.hasText(category)) {
            prompt.append("品类方向：").append(category).append("。\n");
        }
        if (StringUtils.hasText(customInput)) {
            prompt.append("用户补充需求：").append(customInput).append("。\n");
        }
        if (!StringUtils.hasText(category) && !StringUtils.hasText(customInput)) {
            prompt.append("请综合当前季节和市场热点，推荐多个品类方向。\n");
        }
        prompt.append("\n请结合当前季节需求、节日热点、电商趋势，推荐10个适合抖音/淘宝代发的商品搜索关键词。");
        prompt.append("\n每个关键词附带品类和理由。");
        prompt.append("\n只输出JSON数组，格式：[{\"keyword\":\"关键词\",\"category\":\"品类\",\"reason\":\"推荐理由(10字内)\"}]");
        prompt.append("\n不要输出任何其他内容。");

        String result = callDeepSeek(prompt.toString());

        // AI不可用则返回本地规则兜底
        if (result == null) {
            return fallbackKeywords(season, category);
        }
        try {
            int start = result.indexOf('[');
            int end = result.lastIndexOf(']');
            if (start >= 0 && end > start) {
                JSONArray arr = JSONUtil.parseArray(result.substring(start, end + 1));
                List<Map<String, String>> list = new ArrayList<>();
                for (int i = 0; i < arr.size(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    Map<String, String> item = new LinkedHashMap<>();
                    item.put("keyword", obj.getStr("keyword", ""));
                    item.put("category", obj.getStr("category", ""));
                    item.put("reason", obj.getStr("reason", ""));
                    list.add(item);
                }
                return list;
            }
        } catch (Exception e) {
            log.warn("解析推荐关键词失败: {}", e.getMessage());
        }
        return fallbackKeywords(season, category);
    }

    private List<Map<String, String>> fallbackKeywords(String season, String category) {
        Map<String, List<String[]>> seasonMap = new LinkedHashMap<>();
        seasonMap.put("春季", Arrays.asList(
            new String[]{"春季连衣裙", "女装", "换季热销"},
            new String[]{"清洁神器套装", "家居清洁", "春季大扫除"},
            new String[]{"儿童春装套装", "童装", "开学季"},
            new String[]{"花草茶礼盒", "食品", "春日养生"},
            new String[]{"便携折叠雨伞", "出行用品", "春雨多发"}
        ));
        seasonMap.put("夏季", Arrays.asList(
            new String[]{"冰丝凉感T恤", "女装", "夏季必备"},
            new String[]{"便携小风扇", "电子", "消暑神器"},
            new String[]{"防晒霜套装", "美妆护肤", "出行防晒"},
            new String[]{"沙滩短裤男", "男装", "夏日休闲"},
            new String[]{"冰激凌模具", "厨房用品", "DIY消暑"}
        ));
        seasonMap.put("秋季", Arrays.asList(
            new String[]{"秋季针织毛衣", "女装", "秋季换季"},
            new String[]{"保温杯水杯", "生活用品", "秋日保暖"},
            new String[]{"儿童秋季外套", "童装", "开学必备"},
            new String[]{"干果礼盒坚果", "食品", "秋季滋补"},
            new String[]{"加厚毛毯空调毯", "家纺", "秋凉必备"}
        ));
        seasonMap.put("冬季", Arrays.asList(
            new String[]{"羽绒棉服女", "女装", "冬季保暖"},
            new String[]{"电热毯加热垫", "家用电器", "御寒必备"},
            new String[]{"保暖内衣套装", "内衣", "冬日必购"},
            new String[]{"暖手宝充电款", "电子", "冬季热销"},
            new String[]{"卡通加厚棉拖鞋", "鞋履", "居家保暖"}
        ));
        List<String[]> pool = seasonMap.getOrDefault(season, seasonMap.get("夏季"));
        List<Map<String, String>> result = new ArrayList<>();
        for (String[] item : pool) {
            if (StringUtils.hasText(category) && !item[1].contains(category) && !category.contains(item[1])) continue;
            Map<String, String> map = new LinkedHashMap<>();
            map.put("keyword", item[0]);
            map.put("category", item[1]);
            map.put("reason", item[2]);
            result.add(map);
        }
        if (result.isEmpty()) {
            for (String[] item : pool) {
                Map<String, String> map = new LinkedHashMap<>();
                map.put("keyword", item[0]);
                map.put("category", item[1]);
                map.put("reason", item[2]);
                result.add(map);
            }
        }
        return result;
    }
}
