package com.joker.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.joker.reggie.common.R;
import com.joker.reggie.dto.SetmealDto;
import com.joker.reggie.entity.Category;
import com.joker.reggie.entity.Setmeal;

import java.util.List;

public interface SetmealService extends IService<Setmeal> {
    SetmealDto getByIdWithDish(Long id);

    R<String> updateByIdWithDish(SetmealDto setmealDto);

    R<String> removeWithDish(List<Long> ids);
}
