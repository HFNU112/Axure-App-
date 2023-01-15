package com.hmdp.intercepter;

import com.hmdp.utils.UserHolder;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @Author: hsp
 * @Date: 2023/1/12-01-12-23:14
 * @Description: com.hmdp.intercepter
 * @version: 1.0.0
 */
public class LoginIntercepter implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //1.判断是否需要拦截（ThreadLocal中是否由用户）
        if (UserHolder.getUser() == null) {
            //没有用户，需要拦截
            response.setStatus(401);
            return false;
        }
        //有用户，放行
        return true;
    }

}
