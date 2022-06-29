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
}
