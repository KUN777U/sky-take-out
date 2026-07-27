package com.sky.service;

import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.entity.Employee;
import com.sky.result.PageResult;

public interface EmployeeService {

    /**
     * 员工登录
     * @param employeeLoginDTO
     * @return
     */
    Employee login(EmployeeLoginDTO employeeLoginDTO);

    /**
     * 新增员工
     * @param empDTO
     */
    void save(EmployeeDTO empDTO);

    /**
     * 员工分页查询
     * @param employeePageQueryDTO
     * @return
     */
    PageResult page(EmployeePageQueryDTO employeePageQueryDTO);

    /**
     * 员工停用/启用账号
     * @param status
     * @param id
     */
    void startOrStop(Integer status, Long id);
}
