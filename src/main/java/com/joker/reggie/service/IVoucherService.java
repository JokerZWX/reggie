package com.joker.reggie.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.joker.reggie.common.R;
import com.joker.reggie.entity.Voucher;


/**
 * <p>
 *  服务类
 * </p>
 */
public interface IVoucherService extends IService<Voucher> {

    R queryVoucherOfOrder(Long orderId);

    void addSeckillVoucher(Voucher voucher);
}
