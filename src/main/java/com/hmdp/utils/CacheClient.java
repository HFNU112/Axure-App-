package com.hmdp.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.entity.Shop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.hmdp.utils.RedisConstants.*;

/**
 * 缓存工具类
 * @Author: hsp
 * @Date: 2023/2/11-02-11-23:12
 * @Description: com.hmdp.utils
 * @version: 1.0.0
 */
@Slf4j
@Component
public class CacheClient {

    private final StringRedisTemplate stringRedisTemplate;

    //构造器注入
    public CacheClient(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public void set(String key, Object value, Long time, TimeUnit timeUnit){
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), time, timeUnit);
    }

    public void setWithLogicalExepire(String key, Object value, Long time, TimeUnit timeUnit){
        //设置逻辑过期时间
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(timeUnit.toSeconds(time)));
        //写入redis
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }

    //缓存穿透
    public <R, ID> R queryCachePathThrough (
            String keyPrefix, ID id, Class<R> type, Function<ID,R> dbFallback, Long time, TimeUnit timeUnit) {
        String key = keyPrefix + id;
        // 1.从redis查询商户缓存
        String json = stringRedisTemplate.opsForValue().get(key);
        // 2. 判断商铺是否命中
        if (StrUtil.isNotBlank(json)){
            // 3.命中，返回商铺信息
            return JSONUtil.toBean(json, type);
        }
        //3.1 判断命中是否为空值
        if (json != null) {
            return null;
        }
        // 4.redis未命中，根据id查询数据库商铺信息
        R r = dbFallback.apply(id);

        if (r == null){
            //4.1 将空值写入redis
            stringRedisTemplate.opsForValue().set(key, "",  CACHE_NULL_TTL, TimeUnit.MINUTES);
            // 5.返回404
            return null;
        }
        // 6.存在，写入redis  设置缓存过期时间
        this.set(key, r, time, timeUnit);
        // 7.返回商户信息
        return r;
    }


}
