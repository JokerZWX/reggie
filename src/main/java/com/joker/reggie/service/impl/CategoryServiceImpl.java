package com.joker.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.joker.reggie.common.CustomException;
import com.joker.reggie.entity.Category;
import com.joker.reggie.entity.Dish;
import com.joker.reggie.entity.Setmeal;
import com.joker.reggie.mapper.CategoryMapper;
import com.joker.reggie.service.CategoryService;
import com.joker.reggie.service.DishService;
import com.joker.reggie.service.SetmealService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    @Autowired
    private DishService dishService;

    @Autowired
    private SetmealService setmealService;

    @Override
    public void remove(Long id) {
        // 1、查询当前类别id下是否有菜品
        // 1.1、创建LambdaQueryWrapper对象并声明条件
        LambdaQueryWrapper<Dish> dishLambdaQueryWrapper = new LambdaQueryWrapper<>();
        dishLambdaQueryWrapper.eq(Dish::getCategoryId,id);
        // 1.2、根据对应条件进行查询
        int dishCount = dishService.count(dishLambdaQueryWrapper);
        if (dishCount > 0) {
            // 如果有关联，则包抛出异常
            throw new CustomException("当前类别已经关联了菜品，无法删除");
        }
        // 2、查询当前类别id下是否有套餐
        LambdaQueryWrapper<Setmeal> setmealLambdaQueryWrapper = new LambdaQueryWrapper<>();
        setmealLambdaQueryWrapper.eq(Setmeal::getCategoryId,id);
        // 1.2、根据对应条件进行查询
        int SetmealCount = setmealService.count(setmealLambdaQueryWrapper);
        if (SetmealCount > 0) {
            // 如果有关联，则包抛出异常
            throw new CustomException("当前类别已经关联了套餐，无法删除");
        }

        // 3、如果二者都没有，即可直接删除
        removeById(id);
    }
}
