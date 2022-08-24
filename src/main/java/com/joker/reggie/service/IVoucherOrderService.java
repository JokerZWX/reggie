package com.joker.reggie.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.joker.reggie.common.R;
import com.joker.reggie.entity.VoucherOrder;

/**
 * <p>
 *  服务类
 * </p>
 */
public interface IVoucherOrderService extends IService<VoucherOrder> {

    R seckillVoucher(Long voucherId);

//    R createVoucherOrder(Long voucherId);

}
