package com.sky.controller.admin;

import com.sky.dto.OrdersConfirmDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController("adminOrderController")
@RequestMapping("/admin/order")
@Slf4j
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 管理员查询订单管理列表
     * @param ordersPageQueryDTO
     * @return
     */
    @GetMapping("/conditionSearch")
    public Result<PageResult> condidtionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        log.info("ordersPageQueryDTO: {}", ordersPageQueryDTO);
        PageResult pageResult = orderService.conditionsearch(ordersPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 管理员各个订单数量统计信息
     * @return
     */
    @GetMapping("/statistics")
    public Result <OrderStatisticsVO> statistics () {
        log.info("statistics");
        OrderStatisticsVO orderStatisticsVO = orderService.statistics();
        return Result.success(orderStatisticsVO);
    }

    /**
     * 管理员查询订单详情
     * @param id
     * @return
     */
    @GetMapping("/details{id}")
    public Result<OrderVO> details(@PathVariable Long id) {
        log.info("id: {}", id);
        OrderVO orderVO = orderService.details(id);
        return Result.success(orderVO);
    }

    /**
     * 管理员端接单（商家接单其实就是将订单状态信息修改为“已接单”）
     * @param ordersConfirmDTO
     * @return
     */
    @PutMapping("/confirm")
    public Result confirm(@RequestBody OrdersConfirmDTO ordersConfirmDTO) {
        log.info("ordersConfirmDTO: {}", ordersConfirmDTO);
        orderService.confirm(ordersConfirmDTO);
        return Result.success();
    }
}
