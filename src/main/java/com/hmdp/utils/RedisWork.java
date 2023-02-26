package com.hmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * @Author: hsp
 * @Date: 2023/2/26-02-26-14:22
 * @Description: com.hmdp.utils
 * @version: 1.0.0
 */
//全局ID生成类
@Component
public class RedisWork {

    //开始时间戳
    private static final long BEGIN_TIME_STAMP = 1672531200L;

    //序列号位数
    private static final long SEQ_BITS = 32L;

    private StringRedisTemplate stringRedisTemplate;

    public RedisWork(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 生成订单下一位ID
     *
     * @param keyPrefix 每一个订单业务的key前缀
     * @return 订单下一位ID
     */
    public long nextId(String keyPrefix) {
        //1. 当前时间戳
        LocalDateTime currentTime = LocalDateTime.now();
        long currentSecond = currentTime.toEpochSecond(ZoneOffset.UTC);
        long currentTimeStamp = currentSecond - BEGIN_TIME_STAMP;
        //2. 序列号
        //获取当前时间
        String date = currentTime.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        //序列号自增
        Long seqNumber = stringRedisTemplate.opsForValue().increment("INCR:" + keyPrefix + date);
        //3. 返回拼接后的时间戳和序列号
        return currentTimeStamp << SEQ_BITS | seqNumber;
    }
}
