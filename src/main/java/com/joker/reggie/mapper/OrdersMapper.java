package com.joker.reggie.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.joker.reggie.entity.Orders;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrdersMapper extends BaseMapper<Orders> {
}
