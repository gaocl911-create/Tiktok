package org.dromara.creator.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * WeChat mini program configuration for code2Session and phone number decryption.
 */
@Data
@Component
@ConfigurationProperties(prefix = "miniapp.wechat")
public class WeChatMiniappProperties {

    private String appid;
    private String appSecret;
    private String apiBaseUrl = "https://api.weixin.qq.com";
    private Boolean mockEnabled = false;
}
