package com.hmdp.service.impl;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisConstants;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.CACHE_SHOPTYPE_KEY;
import static com.hmdp.utils.RedisConstants.CACHE_SHOPTYPE_TTL;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    // 查询店铺类型
    @Override
    public Result queryList() {
        // 1.从redis查询店铺
        List<String> shoptypes = stringRedisTemplate
                .opsForList().range(CACHE_SHOPTYPE_KEY, 0, -1);

        //创建List单列集合存储店铺类型对象
        ArrayList<ShopType> shopTypeList = new ArrayList<>();
        // 2. 判断redis中店铺类型是否命中
        if (shoptypes.size() != 0){
            // 3.命中，返回店铺类型数据
            //3.1 增强for遍历单列集合，获取每一个店铺对象值 或 迭代器遍历单列集合 或 Lambda表达式遍历单列集合
            for (String shoptype : shoptypes) {
                ShopType shopType = JSONUtil.toBean(shoptype, ShopType.class);
                //将店铺类型依次添加到集合中
                shopTypeList.add(shopType);
            }
            return Result.ok(shopTypeList);
        }
        // 4. 未命中，从数据库店铺类型数据, 根据sort排序
        List<ShopType> shopTypesMysql = query()
                .orderByAsc("sort").list();
        //5. 判断数据库查询的店铺是否存在
        if (shopTypesMysql.size() != 0){
            for (ShopType shopType : shopTypesMysql) {
                // 5.1 存在，写入redis
                //遍历店铺类型List集合shopTypesMysql
                String type = JSONUtil.toJsonStr(shopType);
                //将缓存的店铺类型写入redis
                stringRedisTemplate.opsForList().rightPushAll(CACHE_SHOPTYPE_KEY, type);
                stringRedisTemplate.expire(CACHE_SHOPTYPE_KEY, CACHE_SHOPTYPE_TTL, TimeUnit.MINUTES);
            }
        }
        // 6.返回店铺信息
        return Result.ok(shopTypesMysql);
    }
}
