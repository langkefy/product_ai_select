package com.select.product.service.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.select.product.config.DeepSeekConfig;
import com.select.product.entity.AiCategoryReport;
import com.select.product.mapper.AiCategoryReportMapper;
import com.select.product.service.AiCategoryReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiCategoryReportServiceImpl implements AiCategoryReportService {

    private final DeepSeekConfig deepSeekConfig;
    private final RestTemplate restTemplate;
    private final AiCategoryReportMapper reportMapper;

    @Override
    public AiCategoryReport generateReport(String platform, String category) {
        String usePlatform = StringUtils.hasText(platform) ? platform : "抖音";
        String useCategory = StringUtils.hasText(category) ? category : "全品类";

        String prompt = buildBigPrompt(usePlatform, useCategory);
        String raw = callDeepSeek(prompt, 3000);

        AiCategoryReport report = new AiCategoryReport();
        report.setPlatform(usePlatform);
        report.setCategory(useCategory);
        report.setTitle(usePlatform + " · " + useCategory + " AI选品分析报告 " + LocalDate.now());

        if (raw != null) {
            report.setAiGenerated(1);
            parseAndFill(report, raw);
        } else {
            report.setAiGenerated(0);
            fillFallback(report, usePlatform, useCategory);
        }

        reportMapper.insert(report);
        return report;
    }

    @Override
    public Page<AiCategoryReport> listReports(int page, int size) {
        Page<AiCategoryReport> p = new Page<>(page, size);
        return reportMapper.selectPage(p,
            new LambdaQueryWrapper<AiCategoryReport>().orderByDesc(AiCategoryReport::getCreateTime));
    }

    @Override
    public AiCategoryReport getReport(Long id) {
        return reportMapper.selectById(id);
    }

    // ========== 私有方法 ==========

    private String buildBigPrompt(String platform, String category) {
        String dateStr = LocalDate.now().toString();
        int month = LocalDate.now().getMonthValue();
        String season = month >= 3 && month <= 5 ? "春季" : month >= 6 && month <= 8 ? "夏季" : month >= 9 && month <= 11 ? "秋季" : "冬季";

        return "你是专业的电商选品运营分析师，专注" + platform + "平台，当前日期：" + dateStr + "，季节：" + season + "，品类方向：" + category + "。\n\n" +
            "请输出一份完整的选品运营分析报告，严格按以下JSON格式输出（只输出JSON，不要有任何其他内容）：\n" +
            "{\n" +
            "  \"hotCategories\": [\n" +
            "    {\"name\":\"分类名\", \"reason\":\"热门原因\", \"trend\":\"上升/稳定\", \"competition\":\"高/中/低\"}\n" +
            "  ],\n" +
            "  \"risingCategories\": [\n" +
            "    {\"name\":\"分类名\", \"reason\":\"即将热门原因\", \"timing\":\"预计爆发时间或节点\"}\n" +
            "  ],\n" +
            "  \"productRecommendations\": [\n" +
            "    {\"category\":\"品类\", \"productName\":\"推荐商品名\", \"reason\":\"推荐理由\", \"priceRange\":\"建议售价区间\", \"keySellingPoint\":\"核心卖点\"}\n" +
            "  ],\n" +
            "  \"listingAdvice\": {\n" +
            "    \"title\":\"标题优化建议\",\n" +
            "    \"cover\":\"主图拍摄建议\",\n" +
            "    \"price\":\"定价策略建议\",\n" +
            "    \"tags\":\"商品标签/关键词建议\",\n" +
            "    \"timing\":\"上架时机建议\"\n" +
            "  },\n" +
            "  \"promotionAnalysis\": {\n" +
            "    \"overview\":\"整体推广分析\",\n" +
            "    \"budget\":\"建议推广预算\",\n" +
            "    \"targetAudience\":\"目标人群画像\",\n" +
            "    \"channels\": [{\"name\":\"渠道名\", \"desc\":\"说明\", \"priority\":\"高/中/低\"}]\n" +
            "  },\n" +
            "  \"marketingFlow\": [\n" +
            "    {\"step\":1, \"phase\":\"阶段名称\", \"actions\":[\"动作1\",\"动作2\"], \"duration\":\"时长\", \"kpi\":\"考核指标\"}\n" +
            "  ]\n" +
            "}\n" +
            "要求：hotCategories给出5个，risingCategories给出3个，productRecommendations给出6个，marketingFlow给出5个阶段（冷启动→测款→放量→爆发→复购）。内容需针对" + platform + "店铺实际运营场景，专业且实用。";
    }

    private void parseAndFill(AiCategoryReport report, String raw) {
        try {
            int start = raw.indexOf('{');
            int end = raw.lastIndexOf('}');
            if (start >= 0 && end > start) {
                JSONObject json = JSONUtil.parseObj(raw.substring(start, end + 1));
                report.setHotCategories(safeArr(json, "hotCategories"));
                report.setRisingCategories(safeArr(json, "risingCategories"));
                report.setProductRecommendations(safeArr(json, "productRecommendations"));
                report.setListingAdvice(safeObj(json, "listingAdvice"));
                report.setPromotionAnalysis(safeObj(json, "promotionAnalysis"));
                report.setMarketingFlow(safeArr(json, "marketingFlow"));
            } else {
                setFallbackContent(report, "AI返回格式异常，已使用默认数据");
            }
        } catch (Exception e) {
            log.warn("解析AI报告失败: {}", e.getMessage());
            setFallbackContent(report, "解析异常：" + e.getMessage());
        }
    }

    private String safeArr(JSONObject json, String key) {
        try {
            JSONArray arr = json.getJSONArray(key);
            return arr != null ? arr.toString() : "[]";
        } catch (Exception e) { return "[]"; }
    }

    private String safeObj(JSONObject json, String key) {
        try {
            JSONObject obj = json.getJSONObject(key);
            return obj != null ? obj.toString() : "{}";
        } catch (Exception e) { return "{}"; }
    }

    private void fillFallback(AiCategoryReport report, String platform, String category) {
        setFallbackContent(report, null);
    }

    private void setFallbackContent(AiCategoryReport report, String note) {
        report.setHotCategories("[{\"name\":\"女装\",\"reason\":\"常年高需求\",\"trend\":\"稳定\",\"competition\":\"高\"},{\"name\":\"家居用品\",\"reason\":\"场景化消费兴起\",\"trend\":\"上升\",\"competition\":\"中\"},{\"name\":\"美妆护肤\",\"reason\":\"女性消费力强\",\"trend\":\"上升\",\"competition\":\"高\"},{\"name\":\"食品零食\",\"reason\":\"冲动消费强\",\"trend\":\"稳定\",\"competition\":\"中\"},{\"name\":\"儿童玩具\",\"reason\":\"家长消费意愿强\",\"trend\":\"稳定\",\"competition\":\"中\"}]");
        report.setRisingCategories("[{\"name\":\"宠物用品\",\"reason\":\"养宠人群快速增长\",\"timing\":\"预计未来3个月持续增长\"},{\"name\":\"户外运动装备\",\"reason\":\"健康生活方式趋势\",\"timing\":\"节假日前后爆发\"},{\"name\":\"国潮文创\",\"reason\":\"Z世代文化认同\",\"timing\":\"传统节日前后\"}]");
        report.setProductRecommendations("[{\"category\":\"女装\",\"productName\":\"显瘦连衣裙\",\"reason\":\"女性高频需求\",\"priceRange\":\"69-159元\",\"keySellingPoint\":\"显瘦修身\"},{\"category\":\"家居\",\"productName\":\"多功能收纳盒\",\"reason\":\"整理收纳需求旺盛\",\"priceRange\":\"29-79元\",\"keySellingPoint\":\"大容量省空间\"},{\"category\":\"美妆\",\"productName\":\"保湿面膜套装\",\"reason\":\"复购率高\",\"priceRange\":\"39-99元\",\"keySellingPoint\":\"补水保湿\"},{\"category\":\"食品\",\"productName\":\"网红零食大礼包\",\"reason\":\"礼品属性强\",\"priceRange\":\"49-129元\",\"keySellingPoint\":\"多口味实惠\"},{\"category\":\"儿童\",\"productName\":\"益智积木\",\"reason\":\"教育类消费增长\",\"priceRange\":\"59-199元\",\"keySellingPoint\":\"开发智力\"},{\"category\":\"宠物\",\"productName\":\"宠物零食\",\"reason\":\"宠主高频购买\",\"priceRange\":\"19-59元\",\"keySellingPoint\":\"营养美味\"}]");
        report.setListingAdvice("{\"title\":\"标题前8字放核心关键词，突出使用场景和人群，避免堆砌关键词\",\"cover\":\"主图展示使用场景，真实人物出镜，前3张图重点体现卖点\",\"price\":\"参考竞品定价，初期可略低5%-10%积累评价，稳定后恢复正常价\",\"tags\":\"结合搜索热词，使用类目词+场景词+人群词组合\",\"timing\":\"周四至周日上架，晚7-9点流量高峰期开播推新品\"}");
        report.setPromotionAnalysis("{\"overview\":\"抖音电商以内容为核心，短视频+直播双驱动，结合达人合作与付费投流可快速放量\",\"budget\":\"新店建议启动预算3000-5000元/月，测款阶段每款商品100-300元\",\"targetAudience\":\"18-35岁女性用户为主，关注生活方式、时尚、家居场景\",\"channels\":[{\"name\":\"短视频带货\",\"desc\":\"制作15-60秒产品场景视频，投放DOU+测试自然流量\",\"priority\":\"高\"},{\"name\":\"直播带货\",\"desc\":\"每周3-5场直播，配合限时优惠提升转化\",\"priority\":\"高\"},{\"name\":\"达人合作\",\"desc\":\"找腰部达人（10-100万粉）做置换或分佣合作\",\"priority\":\"中\"},{\"name\":\"千川投流\",\"desc\":\"稳定跑量后开千川定向投放，ROI目标>2\",\"priority\":\"中\"}]}");
        report.setMarketingFlow("[{\"step\":1,\"phase\":\"冷启动期\",\"actions\":[\"完善店铺基础信息和商品详情\",\"发布5-10条产品场景视频\",\"开启直播测试话术和产品\"],\"duration\":\"1-2周\",\"kpi\":\"完播率>30%，互动率>5%\"},{\"step\":2,\"phase\":\"测款期\",\"actions\":[\"小额DOU+测试3-5款商品\",\"分析各商品点击率和转化率\",\"筛选出爆款潜力款\"],\"duration\":\"2-3周\",\"kpi\":\"找到点击率>3%的潜力款\"},{\"step\":3,\"phase\":\"放量期\",\"actions\":[\"对潜力款加大视频更新频率\",\"增加直播时长和频次\",\"开始达人合作引流\"],\"duration\":\"2-4周\",\"kpi\":\"GMV环比增长>50%\"},{\"step\":4,\"phase\":\"爆发期\",\"actions\":[\"开千川付费投流主力款\",\"配合限时活动和优惠券\",\"矩阵号分发内容扩大曝光\"],\"duration\":\"持续运营\",\"kpi\":\"ROI>2，日销破千\"},{\"step\":5,\"phase\":\"复购维护期\",\"actions\":[\"建立私域粉丝群沉淀用户\",\"定期上新保持账号活跃\",\"收集买家评价优化产品\"],\"duration\":\"长期\",\"kpi\":\"复购率>20%，好评率>95%\"}]");
    }

    private String callDeepSeek(String prompt, int maxTokens) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(deepSeekConfig.getKey());

            cn.hutool.json.JSONObject requestBody = new cn.hutool.json.JSONObject();
            requestBody.set("model", deepSeekConfig.getModel());
            requestBody.set("temperature", 0.5);
            requestBody.set("max_tokens", maxTokens);
            cn.hutool.json.JSONArray messages = new cn.hutool.json.JSONArray();
            cn.hutool.json.JSONObject sysMsg = new cn.hutool.json.JSONObject();
            sysMsg.set("role", "system");
            sysMsg.set("content", "你是专业的抖音电商选品运营分析师，输出内容必须是合法的JSON格式");
            messages.add(sysMsg);
            cn.hutool.json.JSONObject userMsg = new cn.hutool.json.JSONObject();
            userMsg.set("role", "user");
            userMsg.set("content", prompt);
            messages.add(userMsg);
            requestBody.set("messages", messages);

            HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);
            ResponseEntity<String> response = restTemplate.exchange(
                deepSeekConfig.getUrl(), HttpMethod.POST, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                cn.hutool.json.JSONObject result = JSONUtil.parseObj(response.getBody());
                cn.hutool.json.JSONArray choices = result.getJSONArray("choices");
                if (choices != null && !choices.isEmpty()) {
                    return choices.getJSONObject(0).getJSONObject("message").getStr("content");
                }
            }
        } catch (Exception e) {
            log.error("调用DeepSeek API失败: {}", e.getMessage());
        }
        return null;
    }
}

