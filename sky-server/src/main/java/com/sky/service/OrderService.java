package com.sky.service;

import com.sky.dto.OrdersConfirmDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.result.PageResult;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;

public interface OrderService {
    /**
     * 用户提交订单
     * @param ordersSubmitDTO
     * @return
     */
    OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO);

    /**
     * 订单支付
     * @param ordersPaymentDTO
     * @return
     */
    OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception;

    /**
     * 支付成功，修改订单状态
     * @param outTradeNo
     */
    void paySuccess(String outTradeNo);

    /**
     * 用户查询历史订单列表,用户端订单分页查询
     * @param page
     * @param pageSize
     * @param status
     * @return
     */
    PageResult page(int page,int pageSize,Integer status);

    /**
     * 用户查询订单详情
     * @param id
     * @return
     */
    OrderVO details(Long id);

    /**
     * 用户取消订单
     * @param id
     */
    void userCancelById(Long id) throws Exception;

    /**
     * 用户再来一单
     * @param id
     */
    void repetition(Long id);



    /**
     * 管理员查询订单管理列表
     * @param ordersPageQueryDTO
     * @return
     */
    PageResult conditionsearch(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 管理员各个订单数量统计信息
     * @return
     */
    OrderStatisticsVO statistics();

    /**
     * 管理员端接单（商家接单其实就是将订单状态信息修改为“已接单”）
     * @param ordersConfirmDTO
     */
    void confirm(OrdersConfirmDTO ordersConfirmDTO);
}
