package com.select.product.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "api99")
public class OpenClawConfig {
    private String apiKey;
    private String url;
}
