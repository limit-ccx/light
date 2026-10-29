package com.zhiguang.config;

import com.zhiguang.interceptor.AdminModuleInterceptor;
import com.zhiguang.interceptor.RateLimitInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    @Autowired
    private RateLimitInterceptor rateLimitInterceptor;

    @Autowired
    private AdminModuleInterceptor adminModuleInterceptor;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 获取项目根目录的绝对路径
        Path absoluteUploadPath = Paths.get(System.getProperty("user.dir")).toAbsolutePath().resolve(uploadDir);
        
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + absoluteUploadPath.toString() + "/");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册限流拦截器，拦截所有 API 接口
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/**")
                .order(1);

        // 后台模块级权限：按职位限制可访问的管理模块
        registry.addInterceptor(adminModuleInterceptor)
                .addPathPatterns("/api/admin/**")
                .order(2);
    }
}
