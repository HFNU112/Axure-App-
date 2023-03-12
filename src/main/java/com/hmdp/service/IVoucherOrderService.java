package com.hmdp.service;

import com.hmdp.dto.Result;
import com.hmdp.entity.VoucherOrder;
import com.baomidou.mybatisplus.extension.service.IService;

public interface IVoucherOrderService extends IService<VoucherOrder> {

    //秒杀优惠券id进行下单
    Result seckillVoucherOrder(Long voucherId);

    //创建优惠券订单
    Result createVoucherOrder(Long voucherId);
}
