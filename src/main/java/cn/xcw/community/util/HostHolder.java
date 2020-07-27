package cn.xcw.community.util;

import cn.xcw.community.entity.User;
import org.springframework.stereotype.Component;

/***
 *当前的用户信息需要被线程内所有方法共享，就用threadLocal保存user
 */
@Component
public class HostHolder {

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
