package com.joker.reggie.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.joker.reggie.entity.Voucher;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author Joker
 * @since 2022-08-22
 */
@Mapper
public interface VoucherMapper extends BaseMapper<Voucher> {

    List<Voucher> queryVoucherOfOrder(@Param("orderId") Long orderId);
}
