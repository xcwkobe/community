package cn.xcw.community.controller;

import cn.xcw.community.entity.Page;
import cn.xcw.community.entity.User;
import cn.xcw.community.service.FollowService;
import cn.xcw.community.service.UserService;
import cn.xcw.community.util.CommunityConstant;
import cn.xcw.community.util.CommunityUtil;
import cn.xcw.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

/**
 * @class: FollowController
 * @author: 邢成伟
 * @description: TODO
 **/

@Controller
public class FollowController implements CommunityConstant{

    @Autowired
    private FollowService followService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private UserService userService;

//    @Autowired
//    private EventProducer eventProducer;

    @PostMapping("/follow")
    @ResponseBody
    public String follow(int entityType,int entityId){
        User user=hostHolder.getUser();
        followService.follow(user.getId(),entityType,entityId);
        //触发关注事件
//        Event event=new Event().setTopic(TOPIC_FOLLOW)
//                .setUserId(hostHolder.getUser().getId())
//                .setEntityType(entityType)
//                .setEntityId(entityId)
//                .setEntityUserId(entityId);
//        eventProducer.fireEvent(event);

        return CommunityUtil.getJsonString(0,"已关注");
    }

    @PostMapping("/unfollow")
    @ResponseBody
    public String unfollow(int entityType,int entityId){
        User user=hostHolder.getUser();
        followService.unfollow(user.getId(),entityType,entityId);
        return CommunityUtil.getJsonString(0,"已取消关注");
    }

    //查出当前用户关注的用户
    @GetMapping("/followees/{userId}")
    public String getFollowees(@PathVariable("userId") int userId, Page page, Model model){
        User user=userService.findUserById(userId);
        if(user==null){
            throw new RuntimeException("用户不存在");
        }
        model.addAttribute("user",user);
        page.setLimit(5);
        page.setPath("/followees/"+userId);
        page.setRows((int)followService.findFolloweeCount(userId,ENTITY_TYPE_USER));
        List<Map<String, Object>> userList = followService.findFollowees(userId, page.getOffset(), page.getLimit());
        //查询出关注的用户，再查出这个用户自己是否也关注了
        if(userList!=null){
            for(Map<String, Object> map:userList){
                User u=(User) map.get("user");
                map.put("hasFollowed",hasFollowed(u.getId()));
            }
        }
        model.addAttribute("users",userList);
        return "/site/followee";
    }

    //查询当前用户(userId)的粉丝
    @GetMapping("/followers/{userId}")
    public String getFollowers(@PathVariable("userId") int userId, Page page, Model model){
        User user=userService.findUserById(userId);
        if(user==null){
            throw new RuntimeException("用户不存在");
        }
        model.addAttribute("user",user);
        page.setLimit(5);
        page.setPath("/followers/"+userId);
        page.setRows((int)followService.findFollowerCount(ENTITY_TYPE_USER,userId));
        List<Map<String, Object>> userList = followService.findFollowers(userId, page.getOffset(), page.getLimit());
        //查询出用户的粉丝，再看有没有
        if(userList!=null){
            for(Map<String, Object> map:userList){
                User u=(User) map.get("user");
                map.put("hasFollowed",hasFollowed(u.getId()));
            }
        }
        model.addAttribute("users",userList);
        return "/site/follower";
    }

    private boolean hasFollowed(int id) {
        //如果用户没登登录，返回false
        if(hostHolder.getUser()==null){
            return false;
        }
        return followService.hasFollowed(hostHolder.getUser().getId(),ENTITY_TYPE_USER,id);
    }
}
