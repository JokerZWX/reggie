package com.joker.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.joker.reggie.common.R;
import com.joker.reggie.entity.Category;
import com.joker.reggie.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 分类管理
 */
@Slf4j
@RestController
@RequestMapping("/category")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;


    @PostMapping
    public R<String> add(@RequestBody Category category){
        log.info("执行添加菜品或套餐操作...");

        categoryService.save(category);
        if (category.getType() == 1){
            return R.success("添加菜品成功！");
        } else if (category.getType() == 2){
            return R.success("添加套餐成功！");
        } else {
            return R.success("未知添加");
        }
    }

    /**
     * 实现分类管理的分页
     * @param page
     * @param pageSize
     * @return
     */
    @GetMapping("/page")
    public R<Page> page(int page, int pageSize){
        // 1、构造分页构造器
        Page<Category> pageInfo = new Page<>(page,pageSize);

        // 按分类类型显示
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.orderBy(true,true,"sort").eq("is_deleted",0);

        // 2、执行查询所有菜品或者套餐
        categoryService.page(pageInfo,queryWrapper);

        return R.success(pageInfo);
    }

    /**
     * 删除类别
     * @param id
     * @return
     */
    @DeleteMapping
    public R<String> delete(Long id){

        categoryService.remove(id);

        return R.success("删除成功！");
    }

    /**
     * 修改分类信息
     * @param category
     * @return
     */
    @PutMapping
    public R<String> update(@RequestBody Category category){
        log.info("执行修改类别信息");
        categoryService.updateById(category);
        return R.success("修改成功！");
    }

    /**
     * 进入添加商品时查询当前所有菜品类别
     * 注意：这里不使用@RequestBody注解是因为请求的参数在请求头，而不是在请求体里面
     */
    @GetMapping("/list")
    public R<List<Category>> list(Category category){
        // 1、创建条件构造器对象
        QueryWrapper<Category> categoryQueryWrapper = new QueryWrapper<>();
        // 2、添加条件 相当于 where type = ?
        categoryQueryWrapper.eq(category.getType() != null,"type",category.getType());
        // 3、再按照sort、updateTime排序
        categoryQueryWrapper.orderByAsc("sort").orderByDesc("update_time");

        // 4、按照上述条件进行查询
        List<Category> list = categoryService.list(categoryQueryWrapper);

        return R.success(list);
    }
}
