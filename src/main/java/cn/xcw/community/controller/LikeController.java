package cn.xcw.community.controller;

import cn.xcw.community.entity.User;
import cn.xcw.community.service.LikeService;
import cn.xcw.community.util.CommunityConstant;
import cn.xcw.community.util.CommunityUtil;
import cn.xcw.community.util.HostHolder;
import cn.xcw.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

/**
 * @class: LikeController
 * @author: 邢成伟
 * @description: TODO
 **/
@Controller
public class LikeController implements CommunityConstant{

    @Autowired
    private LikeService likeService;

    @Autowired
    private HostHolder hostHolder;//得到当前用户

//    @Autowired
//    private EventProducer eventProducer;

    @Autowired
    private RedisTemplate redisTemplate;


    @PostMapping("/like")
    @ResponseBody
    public String like(int entityType,int entityId,int entityUserId,int postId){
        User user = hostHolder.getUser();
        //点赞
        likeService.like(user.getId(),entityType,entityId,entityUserId);

        //点赞数量
        long likeCount = likeService.findEntityLikeCount(entityType, entityId);
        int likeStatus = likeService.findEntityLikeStatus(user.getId(), entityType, entityId);

        //返回结果
        Map<String,Object> map=new HashMap<>();
        map.put("likeCount",likeCount);
        map.put("likeStatus",likeStatus);

        //触发点赞的时间
        if(likeStatus==1){
//            Event event=new Event().setTopic(TOPIC_LIKE)
//                    .setUserId(hostHolder.getUser().getId())
//                    .setEntityType(entityType)
//                    .setEntityId(entityId)
//                    .setEntityUserId(entityUserId)
//                    .setData("postId",postId);
//            eventProducer.fireEvent(event);
        }
        if (entityType==ENTITY_TYPE_POST){
            //计算帖子的分数
            String redisKey= RedisKeyUtil.getPostScoreKey();
            redisTemplate.opsForSet().add(redisKey,postId);
        }

        return CommunityUtil.getJSONString(0,null,map);
    }
}
