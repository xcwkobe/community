package cn.xcw.community.util;

import cn.xcw.community.entity.User;
import org.springframework.stereotype.Component;

/***
 *当前的用户信息需要被线程内所有方法共享，隔离，就用threadLocal保存user
 * 每次浏览器的请求都会开启一个线程
 */
@Component
public class HostHolder {

    //以线程为key存取值得，所以能隔离
    private ThreadLocal<User> users=new ThreadLocal<>();

    public void setUser(User user){
        users.set(user);
    }


    public User getUser(){
        return users.get();
    }


    public void clear(){
        users.remove();
    }
}
