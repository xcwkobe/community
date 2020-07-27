package cn.xcw.community.config;

/**
 * @class: WebMvcConfig
 * @author: 邢成伟
 * @description: TODO
 **/

import cn.xcw.community.intercepter.LoginRequiredInterceptor;
import cn.xcw.community.intercepter.LoginTicketInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 配置登录拦截器的拦截范围
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {


    @Autowired
    private LoginTicketInterceptor loginTicketInterceptor;

    @Autowired
    private LoginRequiredInterceptor loginRequiredInterceptor;
//    @Autowired
//    private MessageInterceptor messageInterceptor;
//
//    @Autowired
//    private DataInteceptor dataInteceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //不拦截静态资源
        registry.addInterceptor(loginTicketInterceptor)//默认拦截所有
                .excludePathPatterns("/**/*.css","/**/*.js","/**/*.png","/**/*.jpg","/**/*.jpeg");

        registry.addInterceptor(loginRequiredInterceptor)
               .excludePathPatterns("/**/*.css","/**/*.js","/**/*.png","/**/*.jpg","/**/*.jpeg");
//        registry.addInterceptor(messageInterceptor)
//                .excludePathPatterns("/**/*.css","/**/*.js","/**/*.png","/**/*.jpg","/**/*.jpeg");
//        registry.addInterceptor(dataInteceptor)
//                .excludePathPatterns("/**/*.css","/**/*.js","/**/*.png","/**/*.jpg","/**/*.jpeg");

    }
}
