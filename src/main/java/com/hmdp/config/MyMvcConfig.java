package com.hmdp.config;

import com.hmdp.intercepter.LoginIntercepter;
import com.hmdp.intercepter.RefreshTokenIntercepter;
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
        //拦截部分的拦截器低优先级
        registry.addInterceptor(new LoginIntercepter())
                .excludePathPatterns(
                        "/blog/hot",
                        "/shop/**",
                        "/shop-type/**",
                        "/user/code",
                        "/user/login"
                ).order(1);
        //拦截所有的拦截器高优先级
        registry.addInterceptor(new RefreshTokenIntercepter(stringRedisTemplate)).addPathPatterns("/**").order(0);
    }
}
