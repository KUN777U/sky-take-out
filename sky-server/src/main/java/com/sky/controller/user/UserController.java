package com.sky.controller.user; // 声明当前类所在的包路径

import com.sky.constant.JwtClaimsConstant; // 导入JWT载荷常量类，存放JWT中存储的key名称（如EMP_ID）
import com.sky.dto.UserLoginDTO; // 导入用户登录请求的DTO，封装前端传来的登录参数（如微信授权码）
import com.sky.entity.User; // 导入User实体类，对应数据库中的user表
import com.sky.properties.JwtProperties; // 导入JWT配置属性类，读取配置文件中的密钥和过期时间
import com.sky.result.Result; // 导入统一响应结果类，用于封装返回给前端的JSON数据
import com.sky.service.UserService; // 导入用户服务接口，定义用户相关的业务逻辑
import com.sky.utils.JwtUtil; // 导入JWT工具类，用于生成和解析JWT令牌
import com.sky.vo.UserLoginVO; // 导入用户登录返回的VO对象，封装登录成功后返回给前端的数据
import io.swagger.annotations.Api; // 导入Swagger的@Api注解，用于标记Controller的分组名称
import io.swagger.annotations.ApiOperation; // 导入Swagger的@ApiOperation注解，用于描述接口的功能
import lombok.extern.slf4j.Slf4j; // 导入Lombok的@Slf4j注解，自动生成log日志对象
import org.springframework.beans.factory.annotation.Autowired; // 导入Spring的@Autowired注解，用于依赖自动注入
import org.springframework.web.bind.annotation.PostMapping; // 导入@PostMapping注解，将HTTP POST请求映射到该方法
import org.springframework.web.bind.annotation.RequestBody; // 导入@RequestBody注解，将请求体中的JSON数据绑定到方法参数
import org.springframework.web.bind.annotation.RequestMapping; // 导入@RequestMapping注解，定义类级别的请求路径前缀
import org.springframework.web.bind.annotation.RestController; // 导入@RestController注解，标记该类为REST控制器（= @Controller + @ResponseBody）

import java.util.HashMap; // 导入HashMap，用于存储JWT的自定义载荷数据（键值对）
import java.util.Map; // 导入Map接口，作为JWT载荷的容器类型

@RestController // 标记该类为REST控制器，所有方法的返回值都会自动转为JSON格式写入响应体
@Api("C端用户接口") // Swagger注解，在knife4j接口文档中，该Controller下的接口会显示在"C端用户接口"分组下
@RequestMapping("/user/user") // 设置该Controller所有接口的请求路径前缀为/user/user
@Slf4j // Lombok注解，编译时自动生成 private static final Logger log = LoggerFactory.getLogger(UserController.class);
public class UserController { // 定义C端用户Controller，处理用户端（微信小程序）的请求

    @Autowired // Spring自动注入，将JwtProperties配置类的Bean赋值给该字段
    private JwtProperties jwtProperties; // JWT配置属性对象，包含密钥（secretKey）和过期时间（ttl），在application.yml中配置

    @Autowired // Spring自动注入，将UserService接口的实现类Bean赋值给该字段
    private UserService userService; // 用户服务对象，提供用户相关的业务逻辑方法（如wxLogin微信登录）

    @PostMapping("/login") // 将HTTP POST请求 /user/user/login 映射到该方法（路径 = 类级别 /user/user + 方法级别 /login）
    @ApiOperation("微信登录") // Swagger注解，在接口文档中描述该接口的功能为"微信登录"
    public Result<UserLoginVO> login(@RequestBody UserLoginDTO userLoginDTO){ // 用户登录方法，返回统一结果对象，参数用@RequestBody接收前端传来的JSON数据
        log.info("用户登录：{}", userLoginDTO); // 打印日志，记录用户登录请求的DTO信息（{}是SLF4J的占位符，会被userLoginDTO的toString结果替换）
        //微信登陆
        User user = userService.wxLogin(userLoginDTO); // 调用UserService的wxLogin方法，传入登录DTO，执行微信登录逻辑，返回完整的User对象
        Map<String, Object> claims = new HashMap<>(); // 创建一个HashMap，用于存储JWT令牌中的自定义载荷数据
        claims.put(JwtClaimsConstant.EMP_ID,user.getId()); // 将用户ID存入JWT载荷中，key为"emp_id"（常量定义），value为用户的ID，后续可通过解析JWT获取当前登录用户
        String token = JwtUtil.createJWT( // 调用JwtUtil工具类生成JWT令牌
                jwtProperties.getUserSecretKey(), // 参数1：用户端的JWT密钥（从配置文件读取，用于签名，保证令牌不被篡改）
                jwtProperties.getUserTtl(), // 参数2：JWT的过期时间（从配置文件读取，单位通常是毫秒）
                claims // 参数3：JWT的自定义载荷数据（包含用户ID等信息）
        );
        UserLoginVO userLoginVO = UserLoginVO.builder() // 使用Builder建造者模式构建UserLoginVO对象（Lombok的@Builder注解生成）
                .id(user.getId()) // 设置返回给前端的用户ID
                .openid(user.getOpenid()) // 设置返回给前端的微信openid（微信用户的唯一标识）
                .token(token) // 设置返回给前端的JWT令牌（前端后续请求需要携带此token进行身份验证）
                .build(); // 调用build()方法完成构建，生成UserLoginVO对象
        return Result.success(userLoginVO); // 调用Result的静态方法success，将userLoginVO封装为统一成功响应返回给前端
    }
}