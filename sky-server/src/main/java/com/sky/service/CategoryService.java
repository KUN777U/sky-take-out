package com.sky.service;

import com.sky.dto.CategoryDTO;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.entity.Category;
import com.sky.result.PageResult;

import java.util.List;

public interface CategoryService {
    /**
     * 修改分类
     * @param categoryDTO
     */
    void update(CategoryDTO categoryDTO);

    /**
     * 分页查询分类
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
     * 批量删除分类
     * @param id
     */
    void deleteById(Long id);

    /**
     * 新增套餐分类
     * @param categoryDTO
     */
    void add(CategoryDTO categoryDTO);

    /**
     * 根据ID查询套餐分类
     * @param id
     * @return
     */
    Category getById(Long id);
}
