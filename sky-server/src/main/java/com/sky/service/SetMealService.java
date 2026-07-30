package com.sky.service;

import com.sky.dto.CategoryDTO;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.entity.Category;
import com.sky.result.PageResult;

public interface SetMealService {

    /**
     * 分页查询套餐
     * @param categoryPageQueryDTO
     * @return
     */
    PageResult page(CategoryPageQueryDTO categoryPageQueryDTO);

    /**
     * 套餐起售、停售
     * @param status
     * @param id
     */
    void startOrStop(Integer status, Long id);

    /**
     * 删除套餐
     * @param id
     */
    void deleteById(Long id);

    /**
     * 新增套餐
     * @param categoryDTO
     */
    void add(CategoryDTO categoryDTO);

    /**
     * 根据ID查询套餐
     * @param id
     * @return
     */
    Category getById(Long id);
}
