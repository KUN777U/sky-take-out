package com.sky.controller.admin;


import com.sky.dto.CategoryDTO;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.dto.SetmealDTO;
import com.sky.entity.Category;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequestMapping("/admin/setmeal")
public class SetMealController {

    @Autowired
    private SetmealService setMealService;


    /**
     * 套餐分页查询
     * @param categoryPageQueryDTO
     */

    @GetMapping("/page")
    public Result page(CategoryPageQueryDTO categoryPageQueryDTO){
        log.info("分页查询分类，categoryPageQueryDTO={}",categoryPageQueryDTO);
        PageResult pageResult = setMealService.page(categoryPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 套餐起售、停售
     */
    @PutMapping("/status/{status}")
    @CacheEvict(cacheNames = "setmaelCache",allEntries = true)
    public Result startOrStop(@PathVariable ("status")Integer status,@RequestParam Long id){
        log.info("员工停用/启用账号,{},{}",status,id);
        setMealService.startOrStop(status,id);
        return Result.success();
    }


    /**
     * 删除套餐
     */
    @DeleteMapping
    @CacheEvict(cacheNames = "setmaelCache",allEntries = true)
    public Result delete(@RequestParam Long id){
        log.info( "删除套餐，id={}",id);
        setMealService.deleteById(id);
        return Result.success();
    }
    /**
     * 新增套餐
     * @param
     */
    @PostMapping
    @CacheEvict(cacheNames = "setmaelCache",key = "#categoryDTO.categoryId")
    public Result add(@RequestBody CategoryDTO categoryDTO){
        log.info("新增套餐，categoryDTO={}",categoryDTO);
        setMealService.add(categoryDTO);
        return Result.success();
    }
    /**
     * 根据ID查询套餐
     * @param
     */
    @GetMapping("/{id}")
    public Result<SetmealVO> getId(@PathVariable Long id){
        SetmealVO setmealVO = setMealService.getByIdWithDish(id);
        return Result.success(setmealVO);
    }
    /**
     * 修改套餐
     *
     * @param setmealDTO
     * @return
     */
    @PutMapping
    @ApiOperation("修改套餐")
    @CacheEvict(cacheNames = "setmaelCache",allEntries = true)
    public Result update(@RequestBody SetmealDTO setmealDTO) {
        setMealService.update(setmealDTO);
        return Result.success();
    }
}
