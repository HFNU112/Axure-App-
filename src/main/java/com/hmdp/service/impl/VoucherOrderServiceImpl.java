package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisWork;
import com.hmdp.utils.UserHolder;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Autowired
    private RedisWork redisWork;

    @Autowired
    private ISeckillVoucherService seckillVoucherService;

    @Override
    public Result seckillVoucherOrder(Long voucherId) {
        // 1.根据优惠券id查询优惠券信息
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);

        // 2.判断优惠券是否到秒杀时间
        if (voucher.getBeginTime().isAfter(LocalDateTime.now())){
            //如果没到时间了
            return Result.fail("尚未到抢购时间，请您稍后在来...");
        }
        if (voucher.getEndTime().isBefore(LocalDateTime.now())){
            // 3.如果到时间了，结束抢购
            return Result.fail("本次活动已结束，请您关注后续...");
        }
        // 4.判断库存是否充足
        if (voucher.getStock() < 1) {
            return Result.fail("很抱歉，库存已经不足了！");
        }
        // 返回优惠券订单
        Long userId = UserHolder.getUser().getId();
        //给订单加同步锁
        synchronized (userId.toString().intern()) {
            // 获取代理对象 （订单提交事务）
            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
            return proxy.createVoucherOrder(voucherId);
        }
    }

    @Transactional
    public Result createVoucherOrder(Long voucherId) {
        // 5. 实现一人下一单
        // 用户id
        Long userId = UserHolder.getUser().getId();
        // 5.1根据优惠券id和用户id连接查询订单
        int count = query()
                .eq("user_id", userId)
                .eq("voucher_id", voucherId)
                .count();
        //5.2 判断订单是否存在
        if (count > 0) {
            // 提示用户不能重复下单
            return Result.fail("用户不能下单！");
        }

        // 6. 库存充足，修改库存
        //UPDATE tb_seckill_voucher SET stock = stock - 1 WHERE (voucher_id = ? and stock > 0)
        boolean isSuccess = seckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherId).gt("stock", 0)
                .update();

        if (!isSuccess) {
            return Result.fail("很抱歉，库存不足");
        }

        // 7.新增订单
        VoucherOrder voucherOrder = new VoucherOrder();
        // 7.1 订单id
        long orderId = redisWork.nextId("voucher_order");
        voucherOrder.setId(orderId);
        voucherOrder.setUserId(userId);
        // 7.3优惠券id
        voucherOrder.setVoucherId(voucherId);
        //保存到订单表
        save(voucherOrder);
        // 8. 返回订单id
        return Result.ok(orderId);
    }
}
