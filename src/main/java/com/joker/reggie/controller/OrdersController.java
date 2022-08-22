package com.joker.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.joker.reggie.common.BaseContext;
import com.joker.reggie.common.R;
import com.joker.reggie.entity.Orders;
import com.joker.reggie.service.OrdersService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/order")
public class OrdersController {
    @Autowired
    private OrdersService orderService;

    @PostMapping("/submit")
    public R<String> submit(@RequestBody Orders orders){
        return orderService.submit(orders);
    }

    /**
     * 修改用户订单状态
     * @param
     * @return
     */
    @PutMapping
    public R<Orders> delivery(@RequestBody Orders orders){
        // 1、如果为空，直接返回
        if (orders == null) {
            log.error("订单为空");
            return R.error("订单为空");
        }
        // 2、获取当前订单状态
        int status = orders.getStatus();
        // 3、修改订单状态
        // 3.1、如果当前状态为“正在派送”，则再次修改时为“已派送”
        // 3.2、如果当前状态为”已派送“，则再次修改时为”已完成“
        // 3.3、如果当前状态为”已完成“，则再次修改时为”已取消“
        orders.setStatus(status + 1);
        boolean isSuccess = orderService.updateById(orders);
        if (!isSuccess) {
            return R.error("修改失败，请重试");
        }
        return R.success(orders);
    }


    /**
     * 显示用户所有订单信息
     * TODO 显示有问题，目前发现好像是前端代码有错误。。
     * @param page
     * @param pageSize
     * @return
     */
    @GetMapping("/userPage")
    public R<Page> userPage(int page, int pageSize){
        // 1、获取当前用户id
        Long userId = BaseContext.getCurrentId();
        Page<Orders> pageInfo = new Page<>(page,pageSize);

        // 2、根据userId查询当前用户所有订单信息
        Page<Orders> ordersPage = orderService.page(pageInfo,
                new LambdaQueryWrapper<Orders>().eq(Orders::getUserId, userId)
                        .orderByDesc(Orders::getOrderTime)
        );
        if (ordersPage == null) {
            return R.error("没有任何订单信息");
        }
        return R.success(ordersPage);
    }


    /**
     * 显示所有订单信息
     * @param page
     * @param pageSize
     * @return
     */
    @GetMapping("/page")
    public R<Page> page(int page, int pageSize,Long number, String beginTime, String endTime){
        Page<Orders> pageInfo = new Page<>(page,pageSize);
        // 2、查询所有订单信息，并按倒序排列
        // 日期不存在
        Page<Orders> ordersPage = null;
        if ((beginTime == null || endTime == null) && number == null) {
            ordersPage = orderService.page(pageInfo,
                    new LambdaQueryWrapper<Orders>()
                            .orderByDesc(Orders::getOrderTime)
            );
        }
        if ((beginTime != null && endTime != null) && number == null) {
            ordersPage = orderService.page(pageInfo,
                    new LambdaQueryWrapper<Orders>()
                            .orderByDesc(Orders::getOrderTime)
                            // 根据起始和终止时间查询订单信息
                            .between(Orders::getOrderTime,beginTime,endTime)
            );
        }
        if ((beginTime == null || endTime == null) && number != null) {
            ordersPage = orderService.page(pageInfo,
                    new LambdaQueryWrapper<Orders>()
                            .orderByDesc(Orders::getOrderTime)
                            .eq(Orders::getNumber,number)
            );
        }
        if (ordersPage == null) {
            return R.error("没有任何订单信息");
        }
        return R.success(ordersPage);
    }

}
