package cn.xcw.community.controller;

import cn.xcw.community.entity.DiscussPost;
import cn.xcw.community.entity.Page;
import cn.xcw.community.entity.User;
import cn.xcw.community.service.DiscussPostService;
import cn.xcw.community.service.LikeService;
import cn.xcw.community.service.UserService;
import cn.xcw.community.util.CommunityConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @class: HomeController
 * @author: 邢成伟
 * @description: 处理首页的逻辑
 **/

@Controller
public class HomeController implements CommunityConstant {

    @Autowired
    private UserService userService;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private LikeService likeService;

    //跳转到首页之前要做的事
    @GetMapping("/index")
    public String toIndexPage(Model model, Page page, @RequestParam(name="orderMode",defaultValue = "0") int orderMode){
        //在方法调用之前，springmvc会自动实例化Model和Page，并且将Page注入到Model中
        //所以在thymeleaf中可以直接访问到page对象的数据

        //设置数据总数（用于计算总页数），userId为0就是查出所有的帖子数量，供首页使用
        page.setRows(discussPostService.findDiscussPostRows(0));
        //复用当前路径，参数直接加在当前请求后，orderMode是根据是最新0，还是最热1排序，最热按分数排
        page.setPath("/index?orderMode="+orderMode);

        //userId为0，就是查询首页的帖子
        List<DiscussPost> list = discussPostService.findDiscussPosts(0, page.getOffset(), page.getLimit(),orderMode);
        //返回的数据中包括post，user，还有点赞数，用map来区分
        List<Map<String,Object>> discussPosts=new ArrayList<>();
        if(list!=null){
            for(DiscussPost post:list){
                Map<String,Object> map=new HashMap<>();
                map.put("post",post);
                //外键可以先查出来再把对象返回
                User user = userService.findUserById(post.getUserId());
                map.put("user",user);
                //获取当前帖子的点赞数
                long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST, post.getId());
                map.put("likeCount",likeCount);
                discussPosts.add(map);
            }
        }
        model.addAttribute("discussPosts",discussPosts);
        model.addAttribute("orderMode",orderMode);
        return "/index";
    }

    @GetMapping("/error")
    public String getErrorPage(){
        return "/error/500";
    }

    @GetMapping("/denied")
    public String getDeniedPath(){
        return "/error/404";
    }

}
