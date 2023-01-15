package com.hmdp.redis;

import org.junit.Test;
import redis.clients.jedis.Jedis;

import java.util.List;

/**
 * @Author: hsp
 * @Date: 2023/1/16-01-16-0:07
 * @Description: com.hmdp.redis
 * @version: 1.0.0
 */
public class jedisTest {

    @Test
    public void jedis() {
        Jedis jedis = new Jedis("192.168.171.132", 6379);
        jedis.lpush("shoptype", "美食", "花店");

        List<String> shopList = jedis.lrange("shoptype", 0, -1);
        System.out.println(shopList);

        jedis.close();
    }

}
