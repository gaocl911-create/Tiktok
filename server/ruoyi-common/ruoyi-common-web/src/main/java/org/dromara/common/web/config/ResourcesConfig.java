package org.dromara.common.web.config;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import org.dromara.common.core.utils.ObjectUtils;
import org.dromara.common.web.handler.GlobalExceptionHandler;
import org.dromara.common.web.interceptor.PlusWebInvokeTimeInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * 通用配置
 *
 * @author Lion Li
 */
@AutoConfiguration
public class ResourcesConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 全局访问性能拦截
        registry.addInterceptor(new PlusWebInvokeTimeInterceptor());
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        // 全局日期格式转换配置
        registry.addConverter(String.class, Date.class, source -> {
            DateTime parse = DateUtil.parse(source);
            if (ObjectUtils.isNull(parse)) {
                return null;
            }
            return parse.toJdkDate();
        });
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
    }

    /**
     * 允许跨域的 origin 列表（逗号分隔）。
     * 留空 → 关闭 CORS（拒绝所有跨域请求）。
     * 单值 "*" → 允许任意来源但禁用 credentials（dev/local 默认行为）。
     * 显式域名 → 按白名单匹配且允许携带 credentials（prod 必须走此分支）。
     */
    @Value("${cors.allowed-origins:*}")
    private String allowedOrigins;

    /**
     * 跨域配置
     */
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
            .map(s -> s.trim())
            .filter(s -> !s.isEmpty())
            .toList();

        if (origins.size() == 1 && "*".equals(origins.get(0))) {
            // 通配模式：禁用 credentials（CORS 规范禁止 * + credentials 组合）。
            config.setAllowCredentials(false);
            config.addAllowedOriginPattern("*");
        } else {
            // 白名单模式：精确匹配，允许携带 cookie / Authorization。
            config.setAllowCredentials(true);
            origins.forEach(config::addAllowedOrigin);
        }

        // 设置访问源请求头
        config.addAllowedHeader("*");
        // 设置访问源请求方法
        config.addAllowedMethod("*");
        // 有效期 1800秒
        config.setMaxAge(1800L);
        // 添加映射路径，拦截一切请求
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        // 返回新的CorsFilter
        return new CorsFilter(source);
    }

    /**
     * 全局异常处理器
     */
    @Bean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }
}
