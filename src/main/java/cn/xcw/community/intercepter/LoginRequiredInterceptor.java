package cn.xcw.community.intercepter;

/**
 * @class: LoginRequiredInterceptor
 * @author: 邢成伟
 * @description: TODO
 **/

import cn.xcw.community.annotation.LoginRequired;
import cn.xcw.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.method.HandlerMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;

/**
 * 拦截所有请求，只处理带有@LoginRequired注解的方法
 */
@Component
public class LoginRequiredInterceptor implements HandlerInterceptor {

    @Autowired
    private HostHolder hostHolder;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (handler instanceof HandlerMethod){
            HandlerMethod handlerMethod=(HandlerMethod) handler;
            Method method = handlerMethod.getMethod();//得到所有方法
            LoginRequired loginRequired = method.getAnnotation(LoginRequired.class);
            if(loginRequired!=null && hostHolder.getUser()==null){
                response.sendRedirect(request.getContextPath()+"/login");
                return false;
            }
        }
        return true;
    }
}
