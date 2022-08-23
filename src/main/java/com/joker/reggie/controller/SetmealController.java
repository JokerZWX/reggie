package com.joker.reggie.controller;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.joker.reggie.common.R;
import com.joker.reggie.dto.SetmealDto;
import com.joker.reggie.entity.Setmeal;
import com.joker.reggie.entity.SetmealDish;
import com.joker.reggie.service.SetmealDishService;
import com.joker.reggie.service.SetmealService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.joker.reggie.utils.RedisConstants.*;

@Slf4j
@RestController
@RequestMapping("/setmeal")
public class SetmealController {

    @Autowired
    private SetmealService setmealService;

    @Autowired
    private SetmealDishService setmealDishService;

    @Autowired
    private RedisTemplate redisTemplate;

    @GetMapping("/page")
    public R<Page> page(int page, int pageSize,String name){
        // 1、创建Page对象，声明每页的大小和当前页
        Page<Setmeal> pageInfo = new Page<>(page,pageSize);
        Page<SetmealDto> setmealDtoPage = new Page<>(page,pageSize);
        // 2、创建条件构造器并声明条件
        LambdaQueryWrapper<Setmeal> lambdaQueryWrapper = new LambdaQueryWrapper<>();

        lambdaQueryWrapper.like(name != null, Setmeal::getName,name);
        // 添加逻辑删除条件
        lambdaQueryWrapper.orderByDesc(Setmeal::getUpdateTime).eq(Setmeal::getIsDeleted,0);

        // 3、按照上述条件进行查询
        setmealService.page(pageInfo,lambdaQueryWrapper);

        // 4、进行对象拷贝，不需要records是因为对象类型不同，所以需要重新组装setmealDto对象的信息
        BeanUtils.copyProperties(pageInfo,setmealDtoPage,"records");

        // 5、得到SetmealDto集合
        List<Setmeal> records = pageInfo.getRecords();
        // 6、创建SetmealDto集合，用户收集SetmealDto对象
        List<SetmealDto> setmealDtoList = new ArrayList<>();
        // 7、依次获取类别名称和套餐的所有菜品
        for (Setmeal setmeal : records) {
            SetmealDto setmealDto = setmealService.getByIdWithDish(setmeal.getId());
            // 添加到集合中
            setmealDtoList.add(setmealDto);
        }

        // 放入SetmealDto的records中
        setmealDtoPage.setRecords(setmealDtoList);

        return R.success(setmealDtoPage);
    }

    @PostMapping
    @Transactional
    public R<String> add(@RequestBody SetmealDto setmealDto){
        // 1、拷贝setmeal对象相应的值赋给setmeal对象
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDto,setmeal);
        // 2、保存setmeal对象到数据库中
        boolean save = setmealService.save(setmeal);
        if (!save){
            return R.error("保存失败！");
        }
        // 3、获取setmealDto中的所有菜品信息
        List<SetmealDish> setmealDishes = setmealDto.getSetmealDishes();

        // 将已经保存到数据生成的setmealId赋给套餐菜品表中的setmeal_id
        setmealDishes = setmealDishes.stream().map(item -> {
            item.setSetmealId(setmeal.getId());
            return item;
        }).collect(Collectors.toList());
        //
        // 4、将其保存到setmeal_dish表中
        boolean isSaved = setmealDishService.saveBatch(setmealDishes);
        if (!isSaved){
            return R.error("保存失败！");
        }
        return R.success("添加套餐成功！");
    }

    @GetMapping("/{id}")
    public R<SetmealDto> getById(@PathVariable Long id){
        SetmealDto setmealDto = setmealService.getByIdWithDish(id);
        if (setmealDto == null){
            R.error("加载套餐内容失败！");
        }
        return R.success(setmealDto);
    }

    /**
     * 修改套餐信息
     * @param setmealDto
     * @return
     */
    @PutMapping
    public R<String> update(@RequestBody SetmealDto setmealDto){
        return setmealService.updateByIdWithDish(setmealDto);
    }

    /**
     * 修改套餐状态
     */
    @PostMapping("/status/{status}")
    @Transactional
    public R<String> status(@PathVariable Integer status,@RequestParam(value = "ids") List<Long> ids){
        // 1、根据id获取相对应菜品信息
        List<Setmeal> setmealList = setmealService.listByIds(ids);

        // 2、遍历所有菜品信息
        for (Setmeal setmeal : setmealList) {
            // 2、获取当前菜品的状态
            Integer setmealStatus = setmeal.getStatus();
            if (setmealStatus == 0){
                status = 1;
            } else if (setmealStatus == 1){
                status = 0;
            }
            // 2、将修改好的状态信息放入dish中
            setmeal.setStatus(status);
            setmealService.updateById(setmeal);
        }

        return R.success("菜品状态修改成功！");
    }

    /**
     * 批量删除菜品----》逻辑删除
     * @param ids
     * @return
     */
    @DeleteMapping
    @Transactional // 声明为事务
    public R<String> delete(@RequestParam("ids") List<Long> ids){
        // 1、根据id查询到对应的菜品
        List<Setmeal> setmealList = setmealService.listByIds(ids);
        // 2、实现逻辑删除
        for (Setmeal setmeal : setmealList) {
            // 2.1、查询套餐状态，确定是否可以删除
            Integer status = setmeal.getStatus();
            if (status == 1) {
                return R.error("套餐正在售卖中，不可删除！");
            }
            // 2.2、批量修改
            setmeal.setIsDeleted(1);
            setmealService.updateById(setmeal);

            // 2.3、再根据套餐id查询相应菜品
            List<SetmealDish> setmealDishes = setmealDishService.list(new LambdaQueryWrapper<SetmealDish>().eq(SetmealDish::getSetmealId, setmeal.getId()));
            for (SetmealDish setmealDish : setmealDishes) {
                // 2.4、逻辑删除
                setmealDish.setIsDeleted(1);
                setmealDishService.updateById(setmealDish);
            }
        }
        return R.success("删除成功！");
    }

    /**
     * // 根据套餐类别获取相应的套餐信息
     * @param categoryId
     * @param status
     * @return
     */
    @GetMapping("/list")
//    @Cacheable(value = "setmealCache",key = "#setmeal.categoryId + '_' + #setmeal.status + '_' + #setmeal.isDeleted")
    public R<List<Setmeal>> list(Long categoryId,int status){
        // 1、判断当前套餐类别信息是否存在redis中
        String key = SETMEAL_NAME_PREFIX + categoryId + "_" + status + "_" + "0";
        List<Setmeal> list = (List<Setmeal>) redisTemplate.opsForValue().get(key);
        if (null != list && !list.isEmpty()){
            // 2、不为空，直接返回
            return R.success(list);
        }
        if (list != null) {
            // 为空，则给出提示信息
            return R.error("你要查询的套餐不存在！");
        }
        // 3、为null的话，就创建一个新的list对象
        list = new ArrayList<>();
        // 4、声明条件
        LambdaQueryWrapper<Setmeal> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        // 5、查询status为0（正在销售）、isDeleted为0（未删除）、按时间先后顺序排列的套餐类别对应的套餐信息
        lambdaQueryWrapper.eq(Setmeal::getCategoryId,categoryId)
                .eq(Setmeal::getStatus,status)
                .eq(Setmeal::getIsDeleted,0);
        lambdaQueryWrapper.orderByDesc(Setmeal::getUpdateTime);
        // 6、查询相应套餐信息
        list = setmealService.list(lambdaQueryWrapper);
        if (list.isEmpty()) {
            // 如果没有的话，则直接缓存一个空集合，避免一直查询没有的套餐，减少数据库压力
            // 给过期时间设置随机值，避免缓存雪崩
            redisTemplate.opsForValue().set(key,list, CACHE_NULL_TTL + RandomUtil.randomLong(3), TimeUnit.MINUTES);
            return null;
        }
        // 7、有数据，则保存到redis中,并设置60-62分钟的有效时间
        redisTemplate.opsForValue().set(key,list,CACHE_SETMEAL_TTL + RandomUtil.randomLong(3), TimeUnit.MINUTES);
        return R.success(list);
    }
}
