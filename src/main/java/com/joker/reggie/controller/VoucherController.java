package com.joker.reggie.controller;


import com.joker.reggie.common.R;
import com.joker.reggie.entity.Voucher;
import com.joker.reggie.service.IVoucherService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/voucher")
public class VoucherController {

    @Resource
    private IVoucherService voucherService;

    /**
     * 新增普通券
     * @param voucher 优惠券信息
     * @return 优惠券id
     */
    @PostMapping
    public R addVoucher(@RequestBody Voucher voucher) {
        voucherService.save(voucher);
        return R.success(voucher.getId());
    }

    /**
     * 新增秒杀券
     * @param voucher 优惠券信息，包含秒杀信息
     * @return 优惠券id
     */
    @PostMapping("seckill")
    public R addSeckillVoucher(@RequestBody Voucher voucher) {
        voucherService.addSeckillVoucher(voucher);
        return R.success(voucher.getId());
    }

    /**
     * 查询店铺的优惠券列表
     * @param orderId 店铺id
     * @return 优惠券列表
     */
    @GetMapping("/list/{orderId}")
    public R queryVoucherOfOrder(@PathVariable("orderId") Long orderId) {
       return voucherService.queryVoucherOfOrder(orderId);
    }
}
