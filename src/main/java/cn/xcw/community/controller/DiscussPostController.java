package cn.xcw.community.controller;

import cn.xcw.community.entity.*;
import cn.xcw.community.event.EventProducer;
import cn.xcw.community.service.CommentService;
import cn.xcw.community.service.DiscussPostService;
import cn.xcw.community.service.LikeService;
import cn.xcw.community.service.UserService;
import cn.xcw.community.util.CommunityConstant;
import cn.xcw.community.util.CommunityUtil;
import cn.xcw.community.util.HostHolder;
import cn.xcw.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * @class: DiscussPostController
 * @author: 邢成伟
 * @description: TODO
 **/
@Controller
@RequestMapping("/discuss")
public class DiscussPostController implements CommunityConstant{

    @Autowired
    private DiscussPostService discussPostService;
    @Autowired
    private HostHolder hostHolder;
    @Autowired
    private UserService userService;
    @Autowired
    private CommentService commentService;
    @Autowired
    private LikeService likeService;

    @Autowired
    private EventProducer eventProducer;
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 新建帖子
     * @param title
     * @param content
     * @return
     */
    @PostMapping("/add")
    @ResponseBody
    public String addDiscussPost(String title,String content){
        User user = hostHolder.getUser();
        if (user==null){
            //返回的是json
            return CommunityUtil.getJsonString(403,"你还没有登录哦");
        }
        DiscussPost post=new DiscussPost();
        post.setUserId(user.getId());
        post.setTitle(title);
        post.setContent(content);
        post.setCreateTime(new Date());
        discussPostService.addDiscussPost(post);

        //触发发帖事件，新建发帖事件实体
//        Event event=new Event().setTopic(TOPIC_PUBLISH).setUserId(user.getId())
//                .setEntityType(ENTITY_TYPE_POST).setEntityId(post.getId());
//        //kafka发布消息
//        eventProducer.fireEvent(event);
        //计算帖子的分数
        String redisKey= RedisKeyUtil.getPostScoreKey();
        //add可以设置一个key，多个value，存在key的时候，就继续添加value
        redisTemplate.opsForSet().add(redisKey,post.getId());

        return CommunityUtil.getJsonString(0,"发布成功！");
    }

    /**
     * 进入帖子的详情页
     * @param discussPostId
     * @param model
     * @param page
     * @return
     */
    @GetMapping("/detail/{discussPostId}")
    public String getDiscussPost(@PathVariable("discussPostId")int discussPostId, Model model, Page page){
        //帖子
        DiscussPost post = discussPostService.findDiscussPostById(discussPostId);
        model.addAttribute("post",post);
        //作者
        User user = userService.findUserById(post.getUserId());
        model.addAttribute("user",user);

        //点赞数量
        long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST, discussPostId);
        model.addAttribute("likeCount",likeCount);

        //点赞状态，看自己有没有给这个帖子点赞
        int likeStatus= hostHolder.getUser()==null?0:likeService.findEntityLikeStatus(hostHolder.getUser().getId(),ENTITY_TYPE_POST,discussPostId);
        model.addAttribute("likeStatus",likeStatus);

        //评论分页的信息
        page.setLimit(5);
        page.setPath("/discuss/detail/"+discussPostId);
        //根据帖子评论的数量分页
        page.setRows(post.getCommentCount());

        //评论：给帖子的评论 帖子会有很多评论 每个评论又会有很多回复
        //回复：给评论的评论
        //先查到帖子的所有父评论列表
        List<Comment> commentList = commentService.findCommentsByEntity(ENTITY_TYPE_POST, post.getId(), page.getOffset(), page.getLimit());
        //评论vo列表viewobject视图对象
        List<Map<String,Object>> commentVoList=new ArrayList<>();

        if(commentList!=null){
            for(Comment comment:commentList){
                //评论vo，为每一个父评论创建一个map，包含所有信息
                Map<String,Object> commentVo=new HashMap<>();
                //评论
                commentVo.put("comment",comment);
                //作者
                commentVo.put("user",userService.findUserById(comment.getUserId()));

                //点赞数量
                likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_COMMENT, comment.getId());
                commentVo.put("likeCount",likeCount);
                //点赞状态
                likeStatus=hostHolder.getUser()==null?0:likeService.findEntityLikeStatus(hostHolder.getUser().getId(),ENTITY_TYPE_COMMENT,comment.getId());
                commentVo.put("likeStatus",likeStatus);

                //评论的回复列表
                List<Comment> replyList = commentService.findCommentsByEntity(ENTITY_TYPE_COMMENT, comment.getId(), 0, Integer.MAX_VALUE);
                List<Map<String,Object>> replyVoList=new ArrayList<>();
                if(replyList!=null){
                    for(Comment reply:replyList){
                        Map<String,Object> replyVo=new HashMap<>();
                        replyVo.put("reply",reply);
                        replyVo.put("user",userService.findUserById(reply.getUserId()));
                        //记录回复指向的人，只会发生在回复列表中，不会发生在父级评论中
                        User target=reply.getTargetId()==0?null:userService.findUserById(reply.getTargetId());
                        replyVo.put("target",target);
                        //回复的点赞数量
                        likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_COMMENT, reply.getId());
                        replyVo.put("likeCount",likeCount);
                        //对某条回复的点赞状态
                        likeStatus=hostHolder.getUser()==null?0:likeService.findEntityLikeStatus(hostHolder.getUser().getId(),ENTITY_TYPE_COMMENT,reply.getId());
                        replyVo.put("likeStatus",likeStatus);
                        replyVoList.add(replyVo);
                    }
                }
                commentVo.put("replys",replyVoList);
                //查询父级评论下的回复数量
                int replyCount=commentService.findCommentCount(ENTITY_TYPE_COMMENT,comment.getId());
                commentVo.put("replyCount",replyCount);
                commentVoList.add(commentVo);
            }
        }
        model.addAttribute("comments",commentVoList);
        return "/site/discuss-detail";
    }

    //将某个帖子置顶，得有权限
    @PostMapping("/top")
    @ResponseBody
    public String setTop(int id){
        //置顶就是修改帖子的type为1
        discussPostService.updateType(id,1);
        //同步到elasticSearch

        //触发发帖事件
//        Event event=new Event().setTopic(TOPIC_PUBLISH).setUserId(hostHolder.getUser().getId())
//                .setEntityType(ENTITY_TYPE_POST).setEntityId(id);
//        eventProducer.fireEvent(event);

        return CommunityUtil.getJsonString(0);
    }

    //将帖子加精，也得有权限
    @PostMapping("/wonderful")
    @ResponseBody
    public String setWonderful(int id){
        //更新帖子的状态
        discussPostService.updateStatus(id,1);
        //同步到elasticSearch
        //触发发帖事件
//        Event event=new Event().setTopic(TOPIC_PUBLISH).setUserId(hostHolder.getUser().getId())
//                .setEntityType(ENTITY_TYPE_POST).setEntityId(id);
//        eventProducer.fireEvent(event);
        //计算帖子的分数
        String redisKey= RedisKeyUtil.getPostScoreKey();
        redisTemplate.opsForSet().add(redisKey,id);

        return CommunityUtil.getJsonString(0);
    }

    //删除帖子，也得有权限
    @PostMapping("/delete")
    @ResponseBody
    public String setDelete(int id){

        discussPostService.updateType(id,2);
        //同步到elasticSearch
        //触发删帖事件
//        Event event=new Event().setTopic(TOPIC_DELETE).setUserId(hostHolder.getUser().getId())
//                .setEntityType(ENTITY_TYPE_POST).setEntityId(id);
//        eventProducer.fireEvent(event);

        return CommunityUtil.getJsonString(0);
    }
}
