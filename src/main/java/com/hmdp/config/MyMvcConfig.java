package com.hmdp.config;

import com.hmdp.intercepter.LoginIntercepter;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

/**
 * @Author: hsp
 * @Date: 2023/1/12-01-12-23:31
 * @Description: com.hmdp.config
 * @version: 1.0.0
 */
@Configuration
public class MyMvcConfig implements WebMvcConfigurer {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LoginIntercepter(stringRedisTemplate))
                .excludePathPatterns(
                        "/blog/hot",
                        "/shop/**",
                        "/shop-type/**",
                        "/user/code",
                        "/user/login"
                );
    }
}
