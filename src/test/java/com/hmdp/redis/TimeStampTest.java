package com.hmdp.redis;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import javax.annotation.Resources;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * @Author: hsp
 * @Date: 2023/2/26-02-26-14:28
 * @Description: com.hmdp.redis
 * @version: 1.0.0
 */
public class TimeStampTest {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    //计算开始时间戳
    @Test
    public void test() {
        LocalDateTime time = LocalDateTime.of(2023, 01, 01, 00, 00, 00);
        long second = time.toEpochSecond(ZoneOffset.UTC);
        System.out.println(second); //1672531200

        LocalDateTime current = LocalDateTime.now();
        long currentSecond = current.toEpochSecond(ZoneOffset.UTC);
        System.out.println(currentSecond);  //1677425057
        //当前时间戳
        long currentStemp = currentSecond - second;

        //序列号
        String date = time.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        Long seq = stringRedisTemplate.opsForValue().increment("INCR:" + date); //NullPointerException 异常
        System.out.println(seq);
        //拼接
        long timeStemp = currentStemp << 32 | seq;
        System.out.println(timeStemp);
    }

}
