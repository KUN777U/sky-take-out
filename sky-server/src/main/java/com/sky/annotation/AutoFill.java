package com.sky.annotation;

/*
 * 自定义注解，用于表示某个方法需要进行功能字段自动填充处理，实现自动填充功能
 */

import com.sky.enumeration.OperationType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用来节省时间，自动填充创建时间、更新时间、创建人、更新人字段
 * 把@AutoFill放在Mapper方法上（比如insert、update方法）就相当于告诉程序这个方法是用来新增（修改）的
 */
@Target(ElementType.METHOD)  //指定加在方法上
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoFill {
    //定义枚举，用于指定填充数据的操作类型：UPDATE INSERT
    OperationType value();
}
