package cn.xcw.community.intercepter;

import cn.xcw.community.entity.LoginTicket;
import cn.xcw.community.entity.User;
import cn.xcw.community.service.UserService;
import cn.xcw.community.util.CookieUtil;
import cn.xcw.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;

/**
 * @class: LoginTicketInterceptor
 * @author: 邢成伟
 * @description: 检查登录凭证的拦截器，设置它的拦截条件
 **/
@Component
public class LoginTicketInterceptor implements HandlerInterceptor {

    @Autowired
    private UserService userService;

    @Autowired
    private HostHolder hostHolder;

    /**
     * prehandle在controller方法处理前被调用
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //从cookie中获取凭证
        //用cookie中的ticket和redis存的ticket对比，
        String ticket = CookieUtil.getValue(request, "ticket");
        if (ticket != null) {
            //查询redis中的凭证，返回LoginTicket
            LoginTicket loginTicket = userService.findLoginTicket(ticket);
            //如果凭证有效，则添加用户
            if (loginTicket != null && loginTicket.getStatus() == 0 && loginTicket.getExpired().after(new Date())) {
                //根据凭证查询用户
                User user = userService.findUserById(loginTicket.getUserId());
                //在本次请求中持有用户 利用threadLocal hostHolder是单例的
                System.out.println(user);
                hostHolder.setUser(user);

                //构建用户认证的结果，并存入SecurityContext，以便于Security进行授权
//                Authentication authentication=new UsernamePasswordAuthenticationToken(
//                        user,user.getPassword(),userService.getAuthorities(user.getId())
                //);
                //SecurityContextHolder.setContext(new SecurityContextImpl(authentication));

            }
        }
        return true;
    }

    /**
     * posthandle在prehandle方法返回true，controller方法处理完，模板引擎渲染之前执行
     * 可以对modelandview操作
     * @param request
     * @param response
     * @param handler
     * @param modelAndView
     * @throws Exception
     */
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        User user = hostHolder.getUser();
        if (user != null && modelAndView != null) {
            modelAndView.addObject("loginUser", user);
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        //请求结束
        hostHolder.clear();
        //SecurityContextHolder.clearContext();
    }
}
