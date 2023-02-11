package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RedisData;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private CacheClient cacheClient;

    //根据id查询商铺信息
    @Override
    public Result queryById(Long id) {
        //缓存穿透问题
        Shop shop = cacheClient.queryCachePathThrough(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);
        //互斥锁解决缓存击穿问题
        //Shop shop = queryCacheMutex(id);
        //用逻辑过期解决缓存击穿问题
        //Shop shop = queryWithLogicalExpire(id);
        if (shop == null) {
            return Result.fail("很抱歉, 暂无店铺更新, 请期待后续发布 ... ");
        }
        // 7.返回商户信息
        return Result.ok(shop);
    }

    //缓存重建的线程池
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    /**
     * 设置逻辑过期时间查询缓存
     * @param id
     * @return
     */
    public Shop queryWithLogicalExpire (Long id){
        String key = CACHE_SHOP_KEY + id;
        // 1.从redis查询商户缓存
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        // 2. 判断商铺是否命中
        if (StrUtil.isBlank(shopJson)){
            // 3.未命中，返回商铺信息
            return null;
        }
        // 4.命中将json反序列化成对象
        RedisData redisData = JSONUtil.toBean(shopJson, RedisData.class);
        JSONObject data = (JSONObject) redisData.getData();
        Shop shop = JSONUtil.toBean(data, Shop.class);
        LocalDateTime expireTime = redisData.getExpireTime();
        // 5.判断是否过期
        if (expireTime.isAfter(LocalDateTime.now())){
            // 5.1 未过期，直接返回店铺信息
            return shop;
        }
        // 5.2 已过期，需要缓存重建
        // 6. 缓存重建
        // 6.1获取互斥锁
        String lockKey = LOCK_SHOP_KEY + id;
        boolean isLock = acquireLock(lockKey);
        // 6.2 判断是否获取锁成功
        if (isLock) {
            // 6.3 成功，开启独立线程实现缓存重建
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    //重建缓存
                    this.saveShop2Redis(id, 20L);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    //释放锁
                    releaseLock(lockKey);
                }
            });
        }
        // 6.4 失败，返回过期的店铺信息
        return shop;
    }

    /**
     * 缓存击穿问题
     * @param id
     * @return
     */
    public Shop queryCacheMutex (Long id) {
        String key = CACHE_SHOP_KEY + id;
        // 1.从redis查询商户缓存
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        // 2. 判断商铺是否命中
        if (StrUtil.isNotBlank(shopJson)){
            // 3.未命中，返回商铺信息
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
            return null;
        }
        //3.1 判断命中是否为空
        if (shopJson != null) {
            return null;
        }
        // 4 实现缓存重建
        String lockKey = LOCK_SHOP_KEY + id;
        Shop shop = null;
        try {
            // 4.1 获取互斥锁
            boolean isLock = acquireLock(lockKey);
            // 4.2 判断互斥锁是否获取成功
            if (!isLock) {
                // 4.3 获取失败，休眠并重试 【这里线程异常不能抛出！！！只能try...catch...finally】
                Thread.sleep(50);
                //递归从redis中查询
                return queryCacheMutex(id);
            }
            //  4.4 获取成功，根据id查询数据库商铺信息
            shop = getById(id);
            //让线程休眠模拟缓存延迟
            Thread.sleep(200);
            // 5 判断shop是否存在, 返回提示信息
            if (shop == null){
                //将空值写入redis
                stringRedisTemplate.opsForValue().set(key, "",  CACHE_NULL_TTL, TimeUnit.MINUTES);
                //返回是空值  value不可以写null
                return null;
            }
            // 6.存在，写入redis  设置缓存过期时间
            stringRedisTemplate.opsForValue()
                    .set(key, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            // 7 释放互斥锁
            releaseLock(lockKey);
        }
        //8 返回商铺信息
        return shop;
    }

    /**
     * 缓存穿透问题
     * @param id
     * @return
     */
 /*   public Shop queryCachePathThrough (Long id) {
        String key = CACHE_SHOP_KEY + id;
        // 1.从redis查询商户缓存
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        // 2. 判断商铺是否命中
        if (StrUtil.isNotBlank(shopJson)){
            // 3.命中，返回商铺信息
            return JSONUtil.toBean(shopJson, Shop.class);
        }
        //3.1 判断命中是否为空
        if (shopJson != null) {
            return null;
        }
        // 4.redis未命中，根据id查询数据库商铺信息
        Shop shop = getById(id);
        if (shop == null){
            //4.1 将空值写入redis
            stringRedisTemplate.opsForValue().set(key, "",  CACHE_NULL_TTL, TimeUnit.MINUTES);
            // 5.返回404
            return null;
        }
        // 6.存在，写入redis  设置缓存过期时间
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
        // 7.返回商户信息
        return shop;
    }*/

    /**
     * 获取互斥锁
     * @param key
     * @return
     */
    private boolean acquireLock (String key) {
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", LOCK_SHOP_TTL, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    /**
     * 释放互斥锁
     * @param key
     */
    private void releaseLock (String key) {
        stringRedisTemplate.delete(key);
    }

    /**
     * 店铺热点数据写入缓存
     * @param id
     */
    public void saveShop2Redis (Long id, Long expireSeconds) throws InterruptedException {
        // 1.查询店铺数据
        Shop shop = getById(id);
        //模拟缓存重建让线程休眠
        Thread.sleep(200L);
        // 2.封装逻辑过期时间
        RedisData redisData = new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
        // 3.写入redis
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(redisData));
    }

    //更新数据库
    @Override
    @Transactional
    public Result update(Shop shop) {
        Long id = shop.getId();
        if (id == null){
            //id不存在
            return Result.fail("商户编号不存在");
        }
        //1.操作数据库 执行增、删、改 出现异常需要回滚事务
        updateById(shop);
        //2.删除缓存
        stringRedisTemplate.delete(CACHE_SHOP_KEY + shop.getId());
        return Result.ok();
    }
}
