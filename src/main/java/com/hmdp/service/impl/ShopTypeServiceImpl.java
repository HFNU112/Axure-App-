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

import static com.hmdp.utils.RedisConstants.CACHE_SHOPTYPE_KEY;

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
        List<String> shoptypes = stringRedisTemplate.opsForList().range(CACHE_SHOPTYPE_KEY, 0, -1);

        // 2. 判断店铺是否存在
        ArrayList<ShopType> shopTypeList = new ArrayList<>();
        if (shoptypes.size() != 0){
            // 3.存在，返回店铺信息
            for (String shoptype : shoptypes) {
                ShopType shopType = JSONUtil.toBean(shoptype, ShopType.class);
                shopTypeList.add(shopType);
            }
            return Result.ok(shopTypeList);
        }
        // 4.redis不存在，数据库店铺信息, 根据sort排序
        List<ShopType> shopTypesMysql = query().orderByAsc("sort").list();
        // 5.存在，写入redis
        for (ShopType shopType : shopTypesMysql) {
            String type = JSONUtil.toJsonStr(shopType);
            stringRedisTemplate.opsForList().rightPushAll(CACHE_SHOPTYPE_KEY, type);
        }
        // 7.返回店铺信息
        return Result.ok(shopTypesMysql);
    }

}
