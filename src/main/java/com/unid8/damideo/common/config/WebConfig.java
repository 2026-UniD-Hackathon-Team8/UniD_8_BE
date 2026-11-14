package com.unid8.damideo.common.config;

import com.unid8.damideo.common.auth.CurrentUserArgumentResolver;
import com.unid8.damideo.common.auth.DeviceIdInterceptor;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final DeviceIdInterceptor deviceIdInterceptor;
    private final CurrentUserArgumentResolver currentUserArgumentResolver;

    public WebConfig(DeviceIdInterceptor deviceIdInterceptor,
                     CurrentUserArgumentResolver currentUserArgumentResolver) {
        this.deviceIdInterceptor = deviceIdInterceptor;
        this.currentUserArgumentResolver = currentUserArgumentResolver;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(deviceIdInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/users");
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserArgumentResolver);
    }
}
