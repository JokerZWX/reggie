package com.joker.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.joker.reggie.common.R;
import com.joker.reggie.entity.Orders;

public interface OrdersService extends IService<Orders> {
    R<String> submit(Orders orders);
}
