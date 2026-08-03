package com.sky.controller.user;

import com.sky.constant.StatusConstant;
import com.sky.entity.Setmeal;
import com.sky.result.Result;
import com.sky.service.SetmealService;
import com.sky.vo.DishItemVO;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController("userSetmealController")
@RequestMapping("/user/setmeal")
@Slf4j
public class SetmealController {


    @Autowired
    private SetmealService setmealService;
    @GetMapping("/list")
    @ApiOperation("获取套餐列表")
    @Cacheable(cacheNames = "setmaelCache",key = "#categoryId") //key:setmaelCache::10
    public Result<List<Setmeal>> list(Long categoryId){
        log.info("获取套餐列表：{}", categoryId);
        Setmeal setmael = new Setmeal();
        setmael.setCategoryId(categoryId);
        setmael.setStatus(StatusConstant.ENABLE);
        List<Setmeal> list = setmealService.list(setmael);
        return Result.success(list);
    }
    /**
     * 根据套餐id查询包含的菜品列表
     *
     * @param id
     * @return
     */
    @GetMapping("/dish/{id}")
    @ApiOperation("根据套餐id查询包含的菜品列表")
    public Result<List<DishItemVO>> dishList(@PathVariable("id") Long id) {
        List<DishItemVO> list = setmealService.getDishItemById(id);
        return Result.success(list);
    }
}
