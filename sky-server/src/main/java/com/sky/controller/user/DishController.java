package com.sky.controller.user;

import com.sky.constant.StatusConstant;
import com.sky.entity.Dish;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController("userDishController")
@RequestMapping("/user/dish")
@Slf4j
public class DishController {

    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private DishService dishService;
    @GetMapping("/list")
    public Result<List<DishVO>> list(@RequestParam Long categoryId){
        log.info("根据分类id查询菜品列表：{}", categoryId);
        //构造redis中的kye，规则：dish_分类+id
        String key = "idsh_" +  categoryId;
        //查询redis中是否存在菜品数据
        List<DishVO> list =(List<DishVO>) redisTemplate.opsForValue().get(key);
        if(list != null && list.size()>0){
            //如果存在直接返回无需查询数据库
            return Result.success(list);
        }

        Dish dish = new Dish();
        dish.setCategoryId(categoryId);
        dish.setStatus(StatusConstant.ENABLE);
        //如果不存在查询数据库并缓存到redis中
        list = dishService.listWithFlavor(dish);
        redisTemplate.opsForValue().set(key,list);
        return Result.success(list);
    }
}

