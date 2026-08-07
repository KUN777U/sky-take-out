package com.sky.config; // 声明当前类所在的包路径，用于组织和管理Java类

import com.sky.interceptor.JwtTokenAdminInterceptor; // 导入管理端JWT令牌拦截器，用于校验管理员的登录状态
import com.sky.interceptor.JwtTokenUserInterceptor; // 导入用户端JWT令牌拦截器，用于校验C端用户的登录状态
import com.sky.json.JacksonObjectMapper; // 导入自定义的Jackson对象映射器，用于处理JSON序列化和反序列化
import lombok.extern.slf4j.Slf4j; // 导入Lombok的@Slf4j注解，编译时自动生成名为log的日志对象
import org.springframework.beans.factory.annotation.Autowired; // 导入Spring的@Autowired注解，用于依赖自动注入
import org.springframework.context.annotation.Bean; // 导入Spring的@Bean注解，用于将方法的返回值注册为Spring容器中的Bean
import org.springframework.context.annotation.Configuration; // 导入Spring的@Configuration注解，标记该类为配置类
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter; // 导入Jackson的HTTP消息转换器，用于处理JSON格式的请求和响应
import org.springframework.http.converter.HttpMessageConverter; // 导入HTTP消息转换器接口，定义消息转换的标准
import org.springframework.web.servlet.config.annotation.InterceptorRegistry; // 导入拦截器注册表，用于注册和管理拦截器
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry; // 导入资源处理器注册表，用于配置静态资源映射
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport; // 导入Spring MVC配置支持类，提供自定义MVC配置的能力
import springfox.documentation.builders.ApiInfoBuilder; // 导入Swagger的API信息构建器，用于构建接口文档的描述信息
import springfox.documentation.builders.PathSelectors; // 导入Swagger的路径选择器，用于筛选哪些路径生成接口文档
import springfox.documentation.builders.RequestHandlerSelectors; // 导入Swagger的请求处理器选择器，用于筛选哪些Controller生成接口文档
import springfox.documentation.service.ApiInfo; // 导入Swagger的API信息类，封装接口文档的标题、版本、描述等元信息
import springfox.documentation.spi.DocumentationType; // 导入Swagger的文档类型枚举，指定使用的文档规范（如SWAGGER_2）
import springfox.documentation.spring.web.plugins.Docket; // 导入Swagger的Docket类，是生成接口文档的核心配置类

import java.util.List; // 导入Java的List接口，用于存储消息转换器列表

/**
 * 配置类，注册web层相关组件
 * 继承WebMvcConfigurationSupport，可以自定义Spring MVC的拦截器、资源映射、消息转换器等
 */
@Configuration // 标记该类为Spring的配置类，Spring容器启动时会加载该类的配置
@Slf4j // Lombok注解，编译时自动生成 private static final Logger log = LoggerFactory.getLogger(WebMvcConfiguration.class);
public class WebMvcConfiguration extends WebMvcConfigurationSupport { // 定义配置类，继承WebMvcConfigurationSupport以获得自定义MVC配置的能力

    @Autowired // Spring自动注入，将容器中JwtTokenAdminInterceptor类型的Bean赋值给该字段
    private JwtTokenAdminInterceptor jwtTokenAdminInterceptor; // 管理端JWT拦截器实例，用于拦截/admin/**路径的请求并校验token
    @Autowired // Spring自动注入，将容器中JwtTokenUserInterceptor类型的Bean赋值给该字段
    private JwtTokenUserInterceptor jwtTokenUserInterceptor; // 用户端JWT拦截器实例，用于拦截/user/**路径的请求并校验token

    /**
     * 注册自定义拦截器
     * 重写父类方法，向Spring MVC中注册拦截器，指定拦截和放行的路径
     * @param registry 拦截器注册表，用于添加拦截器并配置拦截规则
     */
    @Override // 表示该方法是重写父类的方法，编译器会检查方法签名是否正确
    protected void addInterceptors(InterceptorRegistry registry) { // 重写addInterceptors方法，用于注册自定义拦截器
        log.info("开始注册自定义拦截器..."); // 使用Lombok生成的log对象，记录info级别日志，表示开始注册拦截器
        registry.addInterceptor(jwtTokenAdminInterceptor) // 向拦截器注册表中添加管理端JWT拦截器
                .addPathPatterns("/admin/**") // 配置该拦截器拦截所有以/admin/开头的请求路径（**表示匹配所有子路径）
                .excludePathPatterns("/admin/employee/login"); // 排除管理端登录接口，使其不被拦截（登录不需要token校验）

        registry.addInterceptor(jwtTokenUserInterceptor) // 向拦截器注册表中添加用户端JWT拦截器
                .addPathPatterns("/user/**") // 配置该拦截器拦截所有以/user/开头的请求路径
                .excludePathPatterns("/user/user/login") // 排除用户端登录接口，使其不被拦截
                .excludePathPatterns("/user/shop/status"); // 排除店铺状态查询接口，使其不被拦截（用户无需登录即可查看店铺营业状态）
    }

    /**
     * 通过knife4j（基于Swagger）生成管理端接口文档
     * @return Docket对象，包含管理端接口文档的配置信息
     */
    @Bean // 将方法的返回值注册为Spring容器中的一个Bean，供其他组件使用
    public Docket docket1() { // 定义docket1方法，返回管理端接口文档的Docket配置
        ApiInfo apiInfo = new ApiInfoBuilder() // 创建ApiInfoBuilder构建器，用于构建API文档的元信息
                .title("苍穹外卖项目接口文档") // 设置接口文档的标题
                .version("2.0") // 设置接口文档的版本号
                .description("苍穹外卖项目接口文档") // 设置接口文档的描述信息
                .build(); // 调用build()方法构建出ApiInfo对象
        Docket docket = new Docket(DocumentationType.SWAGGER_2) // 创建Docket对象，指定文档类型为SWAGGER_2（Swagger 2.0规范）
                .groupName("管理端接口文档") // 设置接口文档的分组名称，在knife4j页面上会显示为不同的分组
                .apiInfo(apiInfo) // 设置API文档的元信息（标题、版本、描述等）
                .select() // 调用select()方法返回ApiSelectorBuilder，用于配置哪些接口需要生成文档
                .apis(RequestHandlerSelectors.basePackage("com.sky.controller.admin")) // 指定扫描com.sky.controller.admin包下的所有Controller生成接口文档
                .paths(PathSelectors.any()) // 匹配该包下的所有路径，即所有接口都生成文档
                .build(); // 调用build()方法构建出Docket对象
        return docket; // 返回配置好的Docket对象，Spring会将其注册为Bean
    }

    /**
     * 设置静态资源映射
     * 将classpath下的静态资源映射为可通过URL访问，用于knife4j接口文档页面的资源加载
     * @param registry 资源处理器注册表，用于配置静态资源的映射规则
     */
    @Override // 表示该方法是重写父类的方法
    protected void addResourceHandlers(ResourceHandlerRegistry registry) { // 重写addResourceHandlers方法，配置静态资源映射
        registry.addResourceHandler("/doc.html") // 注册一个资源处理器，当访问/doc.html路径时
                .addResourceLocations("classpath:/META-INF/resources/"); // 将该请求映射到classpath下的META-INF/resources/目录，即返回knife4j的文档页面
        registry.addResourceHandler("/webjars/**") // 注册一个资源处理器，当访问/webjars/**路径时（**匹配所有子路径）
                .addResourceLocations("classpath:/META-INF/resources/webjars/"); // 将该请求映射到classpath下的META-INF/resources/webjars/目录，用于加载knife4j的静态资源（CSS、JS等）
    }

    /**
     * 扩展Spring MVC的消息转换器
     * 在默认的消息转换器列表最前面添加自定义的JSON转换器，用于统一处理日期格式、Long精度等问题
     * @param converters Spring MVC的消息转换器列表
     */
    @Override // 表示该方法是重写父类的方法
    protected void extendMessageConverters(List<HttpMessageConverter<?>> converters) { // 重写extendMessageConverters方法，扩展消息转换器
        //创建一个消息转换器，用于处理JSON格式的请求和响应体
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(); // 实例化Jackson的HTTP消息转换器，负责将Java对象与JSON之间互相转换
        //设置对象映射器，用于将Java对象转换为JSON格式
        converter.setObjectMapper(new JacksonObjectMapper()); // 设置自定义的JacksonObjectMapper对象映射器，处理日期格式化、Long类型转String等特殊逻辑
        //将自定义的消息转换器添加到转换器列表的首位
        converters.add(0, converter); // 将自定义转换器插入到列表的索引0位置（最前面），使其优先级最高，优先处理JSON数据
    }

    /**
     * 通过knife4j（基于Swagger）生成用户端接口文档
     * @return Docket对象，包含用户端接口文档的配置信息
     */
    @Bean // 将方法的返回值注册为Spring容器中的一个Bean
    public Docket docket2() { // 定义docket2方法，返回用户端接口文档的Docket配置
        ApiInfo apiInfo = new ApiInfoBuilder() // 创建ApiInfoBuilder构建器，用于构建API文档的元信息
                .title("苍穹外卖项目接口文档") // 设置接口文档的标题
                .version("2.0") // 设置接口文档的版本号
                .description("苍穹外卖项目接口文档") // 设置接口文档的描述信息
                .build(); // 调用build()方法构建出ApiInfo对象
        Docket docket = new Docket(DocumentationType.SWAGGER_2) // 创建Docket对象，指定文档类型为SWAGGER_2
                .groupName("用户端接口文档") // 设置接口文档的分组名称，在knife4j页面上会显示为"用户端接口文档"分组
                .apiInfo(apiInfo) // 设置API文档的元信息
                .select() // 调用select()方法返回ApiSelectorBuilder，用于配置哪些接口需要生成文档
                .apis(RequestHandlerSelectors.basePackage("com.sky.controller.user")) // 指定扫描com.sky.controller.user包下的所有Controller生成接口文档
                .paths(PathSelectors.any()) // 匹配该包下的所有路径，即所有接口都生成文档
                .build(); // 调用build()方法构建出Docket对象
        return docket; // 返回配置好的Docket对象，Spring会将其注册为Bean
    }
}