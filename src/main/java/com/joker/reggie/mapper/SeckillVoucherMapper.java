package com.joker.reggie.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.joker.reggie.entity.SeckillVoucher;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 秒杀优惠券表，与优惠券是一对一关系 Mapper 接口
 * </p>
 *
 * @author Joker
 * @since 2022-08-22
 */
@Mapper
public interface SeckillVoucherMapper extends BaseMapper<SeckillVoucher> {

}
