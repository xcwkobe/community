package cn.xcw.community.controller;

import cn.xcw.community.entity.Comment;
import cn.xcw.community.entity.DiscussPost;
import cn.xcw.community.service.CommentService;
import cn.xcw.community.service.DiscussPostService;
import cn.xcw.community.util.CommunityConstant;
import cn.xcw.community.util.HostHolder;
import cn.xcw.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Date;

/**
 * @class: CommentController
 * @author: 邢成伟
 * @description: TODO
 **/

@Controller
@RequestMapping("/comment")
public class CommentController implements CommunityConstant {

    @Autowired
    private CommentService commentService;

    @Autowired
    private HostHolder hostHolder;

//    @Autowired
//    private EventProducer eventProducer;

    @Autowired
    private DiscussPostService discussPostService;
    @Autowired
    private RedisTemplate redisTemplate;

    //为帖子添加评论
    @PostMapping("/add/{discussPostId}")
    public String addComment(@PathVariable("discussPostId") int discussPostId, Comment comment){
        comment.setUserId(hostHolder.getUser().getId());
        comment.setStatus(0);
        comment.setCreateTime(new Date());
        commentService.addComment(comment);

        //触发评论的事件，加入到kafka消息队列，告诉用户谁评论了你的哪个帖子
//        Event event=new Event().setTopic(TOPIC_COMMENT)
//                .setUserId(hostHolder.getUser().getId())
//                .setEntityType(comment.getEntityType())
//                .setEntityId(comment.getEntityId())
//                .setData("postId",discussPostId);//帖子的id号 用于跳转到该帖子

        //如果评论的是帖子
        if(comment.getEntityType()==ENTITY_TYPE_POST){
            DiscussPost target = discussPostService.findDiscussPostById(comment.getEntityId());
//            event.setEntityUserId(target.getUserId());

        }else if(comment.getEntityType()==ENTITY_TYPE_COMMENT){
            //如果评论的是评论,那么它的entityId就是父级评论的id，返回的是它的父级评论
            Comment target = commentService.findCommentById(comment.getEntityId());
//            event.setEntityUserId(target.getUserId());
        }
//        eventProducer.fireEvent(event);

        //触发发帖事件
        if(comment.getEntityType()==ENTITY_TYPE_POST){
//            event=new Event().setTopic(TOPIC_PUBLISH).setUserId(comment.getUserId())
//                    .setEntityType(ENTITY_TYPE_POST).setEntityId(discussPostId);
//            eventProducer.fireEvent(event);
            //计算帖子的分数
            String redisKey= RedisKeyUtil.getPostScoreKey();
            redisTemplate.opsForSet().add(redisKey,discussPostId);
        }
        return "redirect:/discuss/detail/"+discussPostId;
    }
}
