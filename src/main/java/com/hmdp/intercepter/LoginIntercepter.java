package com.hmdp.intercepter;

import cn.hutool.core.bean.BeanUtil;
import com.hmdp.dto.UserDTO;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @Author: hsp
 * @Date: 2023/1/12-01-12-23:14
 * @Description: com.hmdp.intercepter
 * @version: 1.0.0
 */
public class LoginIntercepter implements HandlerInterceptor {

    private StringRedisTemplate stringRedisTemplate;

    public LoginIntercepter(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 从请求头中获取token
        //HttpSession session = request.getSession();
        String token = request.getHeader("authorization");
        // 基于token获取redis的User对象
        //Object user = session.getAttribute("user");
        String  key = RedisConstants.LOGIN_USER_KEY + token;
        Map<Object, Object> userMap = stringRedisTemplate.opsForHash()
                .entries(key);
        //判断：用户是否存在
        if (userMap.isEmpty()){
            // 如果不存在，拦截  状态码：401  30x  40x  50x
            response.setStatus(401);
            return false;
        }
        // 将查询hash数据转换成UserDto对象
        UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);

        // 存在用户保存到ThreadLocal中
        UserHolder.saveUser(userDTO);

        // 刷新token有效期
        stringRedisTemplate.expire(key, RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);

        //放行
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {

    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        //登录完成后移除用户
        UserHolder.removeUser();
    }

}
