package com.sky.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.service.OrderService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;

    /**
     * 用户提交订单
     *
     * 整体流程分为5步：
     *   第1步：校验业务数据（地址簿是否存在、购物车是否为空）
     *   第2步：向订单表（orders）插入一条订单记录
     *   第3步：将购物车中的每一条商品，转为订单明细，批量插入订单明细表（order_detail）
     *   第4步：下单成功后，清空当前用户的购物车
     *   第5步：将订单的关键信息封装成 VO 对象，返回给前端展示
     *
     * @param ordersSubmitDTO 前端提交过来的订单数据（包含地址id、支付方式、备注等）
     * @return OrderSubmitVO 返回给前端的结果（订单id、订单号、金额、下单时间）
     */
    @Override
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {

        // ==================== 第1步：业务异常校验 ====================
        // 1.1 根据前端传来的地址id，查询地址簿，确认该地址是否存在
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null) {
            // 地址不存在，无法下单，直接抛出自定义业务异常
            // 全局异常处理器会捕获这个异常，并返回友好的错误提示给前端
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        // 1.2 查询当前登录用户的购物车数据
        // BaseContext.getCurrentId() 是从 ThreadLocal 中获取当前请求的用户id
        // ThreadLocal 可以保证同一个请求的多个方法共享同一个用户id，线程安全
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(userId); // 设置查询条件：只查当前用户的购物车
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(shoppingCart);

        if (shoppingCartList == null || shoppingCartList.isEmpty()) {
            // 购物车为空，没有商品可以下单，抛出自定义业务异常
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        // ==================== 第2步：向订单表插入1条数据 ====================
        // 创建一个新的订单对象
        Orders orders = new Orders();
        // BeanUtils.copyProperties 是 Spring 提供的工具类
        // 作用：将 ordersSubmitDTO 中同名属性的值，拷贝到 orders 对象中
        // 比如：addressBookId、payMethod、amount、remark 等都会自动拷贝
        BeanUtils.copyProperties(ordersSubmitDTO, orders);

        // 手动设置 DTO 中没有的字段
        orders.setOrderTime(LocalDateTime.now());           // 下单时间：取当前时间
        orders.setPayStatus(Orders.UN_PAID);                // 支付状态：未付款
        orders.setStatus(Orders.PENDING_PAYMENT);            // 订单状态：待付款
        orders.setNumber(String.valueOf(System.currentTimeMillis())); // 订单号：用当前时间戳生成，保证唯一
        orders.setPhone(addressBook.getPhone());             // 收货人手机号：从地址簿中取
        orders.setConsignee(addressBook.getConsignee());     // 收货人姓名：从地址簿中取
        orders.setUserId(userId);                            // 下单用户id

        // 执行插入操作，MyBatis 会自动回填主键 id 到 orders 对象中
        orderMapper.insert(orders);

        // ==================== 第3步：向订单明细表插入n条数据 ====================
        // 购物车中有多少种商品，就生成多少条订单明细
        List<OrderDetail> orderDetailList = new ArrayList<>();
        for (ShoppingCart cart : shoppingCartList) {
            // 为购物车中的每一条商品，创建一个对应的订单明细对象
            OrderDetail orderDetail = new OrderDetail();
            // 将购物车中商品的属性（名称、价格、数量、图片等）拷贝到订单明细中
            BeanUtils.copyProperties(cart, orderDetail);
            // 手动设置订单明细所属的订单id（上一步插入后自动回填的id）
            orderDetail.setOrderId(orders.getId());
            // 将当前订单明细加入到列表中
            orderDetailList.add(orderDetail);
        }
        // 批量插入所有订单明细，提高性能（一条SQL插入多条记录，而不是循环单条插入）
        orderDetailMapper.insertBatch(orderDetailList);

        // ==================== 第4步：清空当前用户的购物车 ====================
        // 下单成功后，购物车中的商品已经转为订单，需要清空购物车
        shoppingCartMapper.deleteByUserId(userId);

        // ==================== 第5步：封装VO返回结果 ====================
        // VO（View Object）：视图对象，专门用于返回给前端展示
        // 使用 Builder 模式（建造者模式）构建对象，链式调用，代码更简洁优雅
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(orders.getId())                         // 订单主键id
                .orderTime(orders.getOrderTime())           // 下单时间
                .orderAmount(orders.getAmount())            // 订单总金额
                .orderNumber(orders.getNumber())            // 订单号
                .build();                                   // 最终构建出 OrderSubmitVO 对象
        return orderSubmitVO;
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

        //调用微信支付接口，生成预支付交易单
        JSONObject jsonObject = weChatPayUtil.pay(
                ordersPaymentDTO.getOrderNumber(), //商户订单号
                new BigDecimal(0.01), //支付金额，单位 元
                "苍穹外卖订单", //商品描述
                user.getOpenid() //微信用户的openid
        );

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);
    }

}