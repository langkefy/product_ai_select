package com.select.product.service;

import com.select.product.entity.Product;
import java.util.List;

public interface OpenClawService {
    /** @param filterStr onebound filter 参数，如 "dropshipping,guarantee"，null 或空表示不限 */
    String submitCrawlTask(String platform, String keyword, int maxCount, String filterStr);
    Object getTaskResult(String openClawTaskId);
    List<Product> parseProductData(Object rawData);
}

