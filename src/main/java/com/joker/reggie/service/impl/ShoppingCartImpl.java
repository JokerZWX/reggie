package com.joker.reggie.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.joker.reggie.entity.ShoppingCart;
import com.joker.reggie.mapper.ShoppingCartMapper;
import com.joker.reggie.service.ShoppingCartService;
import org.springframework.stereotype.Service;


@Service
public class ShoppingCartImpl extends ServiceImpl<ShoppingCartMapper, ShoppingCart> implements ShoppingCartService {

}
