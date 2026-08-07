package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.ShoppingCart;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.ShoppingCartService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ShoppingCartServiceImpl implements ShoppingCartService {

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmaelMapper;

    /**
     * 清空购物车
     */
    @Override
    public void cleanShoppingCart() {
        // 1. 清空购物车
        Long userId = BaseContext.getCurrentId();
        shoppingCartMapper.deleteByUserId(userId);
    }

    /**
     * 查询购物车列表
     * @return
     */
    @Override
    public List<ShoppingCart> showShoppingCart() {
        //获取当前微信用户的id
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = ShoppingCart.builder()
                .userId(userId)
                .build();
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);
        return list;
    }

    /**
     * 添加购物车
     * 核心逻辑：先判断购物车中是否已经存在该商品，
     * 如果存在则数量+1，不存在则新增一条购物车记录
     * @param shoppingCartDTO 前端传来的购物车数据（包含菜品id或套餐id）
     */
    @Override
    public void add(ShoppingCartDTO shoppingCartDTO) {
        // 1. 构建查询条件：将DTO中的属性拷贝到ShoppingCart实体中，用于查询购物车中是否已有该商品
        ShoppingCart shoppingCart = new ShoppingCart();
        BeanUtils.copyProperties(shoppingCartDTO, shoppingCart);

        // 2. 设置当前登录用户的id，确保只查询当前用户的购物车数据
        Long userId = BaseContext.getCurrentId();//从线程本地变量中获取当前登录用户的id
        shoppingCart.setUserId(userId);

        // 3. 根据userId + dishId/setmealId 查询购物车中是否已经存在该商品
        List<ShoppingCart> list = shoppingCartMapper.list(shoppingCart);

        // 4. 如果查询结果不为空，说明购物车中已经有该商品，只需将数量+1即可
        if (list != null) {
            ShoppingCart cart = list.get(0);
            // 在原有数量基础上加1
            cart.setNumber(cart.getNumber() + 1);
            // 更新数据库中的数量
            shoppingCartMapper.updateNumberById(cart);
        } else {


            // 5. 购物车中不存在该商品，需要插入一条新记录
            /*
            解释：
            Long dishId = shoppingCartDTO.getDishId(); ：从前端传过来的DTO参数里获取菜品id，
            Dish dish = dishMapper.getById(dishId); ：带着菜品id去数据库里查询菜品的详细信息
            shoppingCart.setName(dish.getName());
            shoppingCart.setImage(dish.getImage());  将菜品的名称、图片、价格赋值给购物车这条记录
            shoppingCart.setAmount(dish.getPrice());

            套餐同理

             */
            Long dishId = shoppingCartDTO.getDishId();
            if (dishId != null) {
                // 添加的是菜品：根据菜品id查询菜品信息，填充名称、图片、价格
                Dish dish = dishMapper.getById(dishId);
                shoppingCart.setName(dish.getName());
                shoppingCart.setImage(dish.getImage());
                shoppingCart.setAmount(dish.getPrice());
                // 添加的是套餐：根据套餐id查询套餐信息，填充名称、图片、价格
            }else{
                Long setmealId = shoppingCartDTO.getSetmealId(); //从前端传过来的参数（DTO）里，把套餐的ID拿出来（setmealId）
                Setmeal setmeal = setmaelMapper.getById(setmealId); //拿着套餐ID（setmealId），去数据库套餐表里把套餐详细信息查询出来
                shoppingCart.setName(setmeal.getName());  //把查出来的套餐名称（setmeal.getName()）、图片（setmeal.getImage()）、价格（setmeal.getPrice()）等信息赋值给购物车这条记录
                shoppingCart.setImage(setmeal.getImage());
                shoppingCart.setAmount(setmeal.getPrice());
            }

            // 5.2 设置初始数量为1，并记录创建时间
            shoppingCart.setNumber(1); //购物车内没有这个商品，第一次加进来所以默认数量就是1，不写这个数据库就会变成null
            shoppingCart.setCreateTime(LocalDateTime.now());

            // 5.3 将新的购物车记录插入数据库
            shoppingCartMapper.insert(shoppingCart);
        }
    }
}
