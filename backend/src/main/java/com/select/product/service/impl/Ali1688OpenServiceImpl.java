package com.select.product.service.impl;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.alibaba.fenxiao.param.*;
import com.alibaba.ocean.rawsdk.ApiExecutor;
import com.alibaba.ocean.rawsdk.common.SDKResult;
import com.alibaba.product.param.*;
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
 * 1688 闃块噷宸村反寮€鏀惧钩鍙伴噰闆嗘湇鍔★紙瀹樻柟 SDK 鐗堬級
 * 鍒嗛攢璇嶆悳: com.alibaba.fenxiao/product.keywords.search
 * 鍟嗗搧璇︽儏: alibaba.product.simpleget
 * 绛惧悕 / 搴忓垪鍖?/ 鍙嶅簭鍒楀寲鍧囩敱 SDK ApiExecutor 鑷姩澶勭悊
 */
@Slf4j
@Service("ali1688OpenService")
public class Ali1688OpenServiceImpl implements OpenClawService {


    private String appKey;
    private String appSecret;
    private String accessToken;

    /** 鐢?CollectServiceImpl 鍔ㄦ€佹敞鍏ュ钩鍙伴厤缃?*/
    public void setConfig(String apiUrl, String appKey, String appSecret) {
        this.appKey = appKey;
        this.appSecret = appSecret;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    /** 姣忔璇锋眰鍒涘缓鏂?ApiExecutor锛堢嚎绋嬪畨鍏級 */
    private ApiExecutor createExecutor() {
        return new ApiExecutor(appKey, appSecret);
    }

    // ==================== filter 鏍囩鏄犲皠 ====================

    /**
     * filterStr 鈫?鍒嗛攢璇嶆悳 filter 鏍囩 String[]
     * 454466: 绾夸笅闄堝垪鍝侊紙鐜拌揣/48h/浠ｅ彂鍖呴偖/7澶╅€€鎹級
     * 454658: 涓ラ€変紭璐ㄥ搧锛堣秼鍔跨垎娆?鍏ㄧ綉浣庝环/鈥︼級
     */
    private String[] buildFenxiaoFilter(String filterStr) {
        if (filterStr == null || filterStr.isBlank()) return new String[0];
        if (filterStr.contains("dropshipping") || filterStr.contains("guarantee") || filterStr.contains("freeshipping")) {
            return new String[]{"454466"};
        }
        return new String[0];
    }

    // ==================== 鎼滅储閲囬泦 ====================

    @Override
    public String submitCrawlTask(String platform, String keyword, int maxCount, String filterStr) {
        // 前置检查
        if (appKey == null || appKey.isBlank()) {
            throw new RuntimeException("1688开放平台 App Key 未配置，请在「采集平台」页面填写 App Key / App Secret");
        }
        if (accessToken == null || accessToken.isBlank()) {
            throw new RuntimeException("1688开放平台 Access Token 未配置。product.keywords.search 接口需要用户授权的 OAuth2 Access Token，请在平台配置中填写有效 Token");
        }

        int pageSize = Math.min(20, maxCount);
        int pages = (int) Math.ceil((double) maxCount / pageSize);

        log.info("==== 1688分销词搜(SDK) 采集请求 ====");
        log.info("  关键词: {}, 数量: {} (每页{}条, 共{}页), appKey={}", keyword, maxCount, pageSize, pages, appKey);

        // 快速连通性测试，避免因网络不通造成长时间阻塞
        try {
            java.net.URL url = new java.net.URL("https://gw.open.1688.com");
            java.net.HttpURLConnection testConn = (java.net.HttpURLConnection) url.openConnection();
            testConn.setConnectTimeout(8000);
            testConn.setReadTimeout(5000);
            testConn.setRequestMethod("HEAD");
            testConn.connect();
            testConn.disconnect();
            log.info("  [连通性] gw.open.1688.com 可访问，HTTP状态: {}", testConn.getResponseCode());
        } catch (Exception connEx) {
            String msg = "无法连接到 1688 开放平台 (gw.open.1688.com)，请检查服务器网络：" + connEx.getMessage();
            log.error("  [连通性] {}", msg);
            throw new RuntimeException(msg, connEx);
        }

        StringBuilder allItems = new StringBuilder("{\"items\":{\"item\":[");
        boolean first = true;
        int totalCollected = 0;
        String[] filterTags = buildFenxiaoFilter(filterStr);
        ApiExecutor executor = createExecutor();

        for (int page = 1; page <= pages && totalCollected < maxCount; page++) {
            try {
                // 内层业务参数
                AlibabaOceanOpenplatformBizProductParamFenXiaoKeyWordSearchParam searchParam =
                        new AlibabaOceanOpenplatformBizProductParamFenXiaoKeyWordSearchParam();
                searchParam.setKeywords(keyword);
                searchParam.setPageNum((long) page);
                searchParam.setPageSize((long) pageSize);
                if (filterTags.length > 0) searchParam.setFilter(filterTags);

                // 外层请求对象
                ProductKeywordsSearchParam param = new ProductKeywordsSearchParam();
                param.setParam(searchParam);

                log.info("  [第{}/{}页] SDK product.keywords.search 发起请求...", page, pages);
                SDKResult<ProductKeywordsSearchResult> sdkResult = executor.execute(param, accessToken);

                if (sdkResult.getErrorCode() != null && !sdkResult.getErrorCode().isEmpty()) {
                    String errCode = sdkResult.getErrorCode();
                    String errMsg  = sdkResult.getErrorMessage();
                    String detail;
                    if (errCode.contains("token") || errCode.contains("auth") || errCode.contains("session")) {
                        detail = "Access Token 无效或已过期 (" + errCode + ": " + errMsg + ")。请在「采集平台」配置页面更新 Access Token。";
                    } else if (errCode.contains("permission") || errCode.contains("forbid") || errCode.contains("isv")) {
                        detail = "API 权限不足 (" + errCode + ": " + errMsg + ")。请确认应用已申请 product.keywords.search (分销词搜) 接口权限，或联系1688开放平台客服开通分销解决方案。";
                    } else {
                        detail = "1688 API 错误: " + errCode + " - " + errMsg;
                    }
                    log.warn("  [第{}页] API返回错误: {}", page, detail);
                    // 任何页数出错都终止，避免无意义重试
                    throw new RuntimeException(detail);
                }

                ProductKeywordsSearchResult result = sdkResult.getResult();
                if (result == null) {
                    String msg = "1688 API 返回空结果 (result=null)，请检查 App Key/Secret 及接口权限";
                    log.warn("  [第{}页] {}", page, msg);
                    if (page == 1) throw new RuntimeException(msg);
                    break;
                }

                AlibabaOpenapiSharedCommonPageResultModel pageResult = result.getResult();
                if (pageResult == null || !Boolean.TRUE.equals(pageResult.getSuccess())) {
                    String msg = pageResult != null ? pageResult.getMessage() : "null";
                    log.warn("  [第{}页] 业务失败: {}", page, msg);
                    if (page == 1) throw new RuntimeException("1688分销词搜业务失败: " + msg);
                    break;
                }

                AlibabaOceanOpenplatformBizProductResultFenXiaoSearchOfferModel[] offers = pageResult.getResult();
                log.info("  [第{}页] 商品数: {}", page, offers == null ? 0 : offers.length);

                if (offers != null) {
                    for (AlibabaOceanOpenplatformBizProductResultFenXiaoSearchOfferModel offer : offers) {
                        if (totalCollected >= maxCount) break;
                        if (!first) allItems.append(",");
                        allItems.append(offerToJson(offer));
                        first = false;
                        totalCollected++;
                    }
                }

                if (offers == null || offers.length < pageSize) break;
                if (page < pages) Thread.sleep(500);

            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                // 网络异常（SocketTimeoutException 等）- 第1页直接失败，后续页跳过
                String msg = e.getClass().getSimpleName() + ": " + e.getMessage();
                log.warn("  [第{}/{}页] 网络/SDK异常: {}", page, pages, msg);
                if (page == 1) throw new RuntimeException("1688 SDK 调用异常（第1页）: " + msg, e);
                break;
            }
        }

        allItems.append("]}}");
        String taskId = "1688fenxiao_" + keyword + "_" + System.currentTimeMillis();
        log.info("  ✅ 采集完成, 商品数: {}", totalCollected);
        log.info("===================================");
        return taskId + "##" + allItems;
    }

    /** 灏?SDK offer 瀵硅薄杞负 JSON 瀛楃涓诧紙涓棿鏍煎紡锛岀敱 parseProductData 缁熶竴瑙ｆ瀽锛?*/
    private String offerToJson(AlibabaOceanOpenplatformBizProductResultFenXiaoSearchOfferModel offer) {
        StringBuilder sb = new StringBuilder("{");
        putStr(sb, "offerId", offer.getOfferId() == null ? "" : String.valueOf(offer.getOfferId()));
        putStr(sb, "subject", offer.getSubject());

        AlibabaOceanOpenplatformBizFenxiaoResultOfferPrice op = offer.getOfferPrice();
        if (op != null) {
            putStr(sb, "_price", op.getPrice());
            putStr(sb, "_consignPrice", op.getConsignPrice());
            putStr(sb, "_priceUnderLine", op.getPriceUnderLine());
            if (Boolean.TRUE.equals(op.getDistributionFreePostage()))
                sb.append("\"_freePost\":true,");
        }

        AlibabaOceanOpenplatformBizFenxiaoResultOfferImage img = offer.getOfferImage();
        if (img != null) putStr(sb, "imageUrl", img.getImageUrl());

        AlibabaOceanOpenplatformBizFenxiaoResultCompanyInfo ci = offer.getCompanyInfo();
        if (ci != null) {
            putStr(sb, "province", ci.getProvince());
            putStr(sb, "city", ci.getCity());
            putStr(sb, "companyName", ci.getCompanyName());
        }

        AlibabaOceanOpenplatformBizFenxiaoResultQualityEvaluation qe = offer.getQualityEvaluation();
        if (qe != null && qe.getCompositeScore() != null)
            sb.append("\"compositeScore\":").append(qe.getCompositeScore()).append(",");

        AlibabaOceanOpenplatformBizFenxiaoResultOfferHistoryTradeInfo[] tradeArr = offer.getOfferHistoryTradeInfo();
        if (tradeArr != null) {
            for (AlibabaOceanOpenplatformBizFenxiaoResultOfferHistoryTradeInfo ti : tradeArr) {
                if (ti != null && ti.getHistoryTradeKey() != null && ti.getHistoryTradeKey().contains("Sales")) {
                    putStr(sb, "_salesValue", ti.getHistoryTradeValue());
                    break;
                }
            }
        }

        if (offer.getQuantityBegin() != null)
            sb.append("\"quantityBegin\":").append(offer.getQuantityBegin()).append(",");

        if (sb.charAt(sb.length() - 1) == ',') sb.deleteCharAt(sb.length() - 1);
        sb.append("}");
        return sb.toString();
    }

    private void putStr(StringBuilder sb, String key, String val) {
        if (val == null) val = "";
        sb.append("\"").append(key).append("\":\"")
          .append(val.replace("\\", "\\\\").replace("\"", "\\\""))
          .append("\",");
    }

    @Override
    public Object getTaskResult(String taskId) {
        if (taskId != null && taskId.contains("##")) return taskId.split("##", 2)[1];
        return null;
    }

    // ==================== 瑙ｆ瀽鍟嗗搧 ====================

    @Override
    public List<Product> parseProductData(Object rawData) {
        List<Product> products = new ArrayList<>();
        if (rawData == null) return products;
        try {
            JSONObject json = JSONUtil.parseObj(rawData.toString());
            JSONObject items = json.getJSONObject("items");
            if (items == null) return products;
            cn.hutool.json.JSONArray itemList = items.getJSONArray("item");
            if (itemList == null) return products;
            LocalDate today = LocalDate.now();
            for (int i = 0; i < itemList.size(); i++) {
                JSONObject item = itemList.getJSONObject(i);
                Product p = new Product();
                parseFenxiaoItem(item, p, today);
                if (p.getTitle() != null && !p.getTitle().isEmpty()) products.add(p);
            }
        } catch (Exception e) {
            log.error("瑙ｆ瀽1688鍒嗛攢璇嶆悳鍟嗗搧鏁版嵁澶辫触", e);
        }
        return products;
    }

    private void parseFenxiaoItem(JSONObject item, Product p, LocalDate today) {
        String productId = item.getStr("offerId", "");
        p.setNumIid(productId);
        p.setTitle(item.getStr("subject", ""));

        String priceStr = item.getStr("_price", item.getStr("_priceUnderLine", "0"));
        try { p.setPrice(new BigDecimal(priceStr)); } catch (Exception e) { p.setPrice(BigDecimal.ZERO); }

        String consign = item.getStr("_consignPrice", "");
        if (!consign.isEmpty()) {
            try { p.setAgentPrice(new BigDecimal(consign)); } catch (Exception ignored) {}
        }

        String prov = item.getStr("province", "");
        String city = item.getStr("city", "");
        p.setLocation(prov + (city.isEmpty() ? "" : " " + city));

        p.setImageUrl(item.getStr("imageUrl", ""));

        if (!productId.isEmpty()) {
            p.setDetailUrl("https://detail.1688.com/offer/" + productId + ".html");
        }

        Double score = item.getDouble("compositeScore");
        p.setRating(score != null ? BigDecimal.valueOf(score) : new BigDecimal("5.0"));

        String salesVal = item.getStr("_salesValue", "");
        if (!salesVal.isEmpty()) {
            try { p.setSales(Long.parseLong(salesVal.replace("+", "").replace(",", "").trim())); }
            catch (Exception ignored) { p.setSales(0L); }
        } else {
            p.setSales(0L);
        }

        Integer qty = item.getInt("quantityBegin");
        p.setMinNum(qty != null ? qty : 1);

        p.setCollectDate(today);
        p.setCollectTime(LocalDateTime.now());
    }

    // ==================== 鍟嗗搧璇︽儏琛ュ叏锛圫DK 鐗堬級====================

    /**
     * 璋冪敤 alibaba.product.simpleget 鑾峰彇鍟嗗搧璇︽儏锛堝彂璐у湴/浠ｅ彂浠?寤鸿闆跺敭浠?7澶╅€€鎹㈢瓑锛?
     */
    public AlibabaProductProductInfo fetchItemDetail(String productId) {
        if (appKey == null || appSecret == null || productId == null || productId.isEmpty()) return null;
        try {
            AlibabaProductSimpleGetParam param = new AlibabaProductSimpleGetParam();
            param.setProductID(Long.parseLong(productId.replace(".0", "")));

            SDKResult<AlibabaProductSimpleGetResult> sdkResult = createExecutor().execute(param, accessToken);

            if (sdkResult.getErrorCode() != null && !sdkResult.getErrorCode().isEmpty()) {
                log.warn("[1688 simpleget] SDK閿欒: {} - {}", sdkResult.getErrorCode(), sdkResult.getErrorMessage());
                return null;
            }

            AlibabaProductSimpleGetResult result = sdkResult.getResult();
            if (result == null) return null;

            if (result.getErrMsg() != null && !result.getErrMsg().isEmpty()) {
                log.warn("[1688 simpleget] 涓氬姟閿欒: productId={}, msg={}", productId, result.getErrMsg());
                return null;
            }

            return result.getProductInfo();
        } catch (Exception e) {
            log.error("[1688 simpleget] 璇锋眰寮傚父: productId={}, err={}", productId, e.getMessage());
            return null;
        }
    }

    /**
     * 灏?AlibabaProductProductInfo 璇︽儏鍥炲～鍒?Product 瀛楁
     */
    public void fillProductFromDetail(Product p, AlibabaProductProductInfo info) {
        if (info == null) return;

        // 鍙戣揣鍦?+ 澶勭悊鏃舵晥锛坔andlingTime 鈮?2澶?鈫?鏀寔48h鍙戣揣锛?
        AlibabaProductProductShippingInfo shipping = info.getShippingInfo();
        if (shipping != null) {
            String addr = shipping.getSendGoodsAddressText();
            if (addr != null && !addr.isBlank()) p.setLocation(addr);
            Integer handling = shipping.getHandlingTime();
            if (handling != null) p.setDeliveryIn48h(handling <= 2 ? 1 : 0);
        }

        // 浠ｅ彂浠?/ 寤鸿闆跺敭浠?/ 鏈€灏忚捣璁㈤噺
        AlibabaProductProductSaleInfo saleInfo = info.getSaleInfo();
        if (saleInfo != null) {
            if (p.getAgentPrice() == null && saleInfo.getConsignPrice() != null && saleInfo.getConsignPrice() > 0) {
                p.setAgentPrice(BigDecimal.valueOf(saleInfo.getConsignPrice()));
            }
            if (saleInfo.getRetailprice() != null && saleInfo.getRetailprice() > 0) {
                p.setPlatformSuggestPrice(BigDecimal.valueOf(saleInfo.getRetailprice()));
            }
            if (p.getMinNum() == null && saleInfo.getMinOrderQuantity() != null) {
                p.setMinNum(saleInfo.getMinOrderQuantity());
            }
        }

        // 7澶╂棤鐞嗙敱閫€鎹?鈫?鍏ㄥ寘鍞悗
        if (Boolean.TRUE.equals(info.getSevenDaysRefunds())) {
            p.setFullAfterSales(1);
        }
    }
}
