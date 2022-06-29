package com.joker.reggie.dto;


import com.joker.reggie.entity.Setmeal;
import com.joker.reggie.entity.SetmealDish;
import lombok.Data;
import java.util.List;

@Data
public class SetmealDto extends Setmeal {

    private List<SetmealDish> setmealDishes;

    private String categoryName;
}
