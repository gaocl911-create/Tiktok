package org.dromara.creator.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * TikHub API configuration.
 */
@Data
@Component
@ConfigurationProperties(prefix = "creator.tikhub")
public class TikHubProperties {

    private Boolean enabled = false;
    private String apiBaseUrl = "https://api.tikhub.io";
    private String apiToken;
    private Integer timeoutSeconds = 15;
    private BigDecimal dailyBudgetUsd = BigDecimal.valueOf(5);
    private BigDecimal estimatedUnitPriceUsd = BigDecimal.valueOf(0.001);
    private BigDecimal usdToCnyRate = BigDecimal.valueOf(7.2);
}
