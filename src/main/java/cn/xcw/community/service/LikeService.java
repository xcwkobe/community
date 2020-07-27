package cn.xcw.community.service;

import cn.xcw.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

/**
 * @class: LikeService
 * @author: 邢成伟
 * @description: TODO
 **/
@Service
public class LikeService {

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 给某个用户实体的点赞就是给这个用户增加赞数
     * 点赞，谁（当前登录用户userId）给谁的（entityUserId）哪个实体（entityId，entityType）点赞
     * @param userId
     * @param entityType
     * @param entityId
     * @param entityUserId
     */
    public void like(int userId, int entityType, int entityId, int entityUserId) {
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
                String userLikeKey = RedisKeyUtil.getUserLikeKey(entityUserId);
                Boolean isMember = redisTemplate.opsForSet().isMember(entityLikeKey, userId);
                //MULTI：标记一个事务块的开始。
                operations.multi();
                if (isMember) {
                    //如果已经点过赞了 就要从key对应的set中移除对应的userId；
                    redisTemplate.opsForSet().remove(entityLikeKey, userId);
                    redisTemplate.opsForValue().decrement(userLikeKey);
                } else {
                    //否则就增加这个实体的赞的数量，同时增加这个用户赞的数量
                    redisTemplate.opsForSet().add(entityLikeKey, userId);
                    redisTemplate.opsForValue().increment(userLikeKey);
                }
                //EXEC：执行所有事务块的命令。
                return operations.exec();
            }
        });
    }

    /**
     * 查询某个用户获得的赞数
     * @param userId
     * @return
     */
    public int findUserLikeCount(int userId) {
        String userLikeKey = RedisKeyUtil.getUserLikeKey(userId);
        Integer count = (Integer)redisTemplate.opsForValue().get(userLikeKey);
        return  count==null?0:count.intValue();
    }

    /**
     * 查询某个实体（帖子，评论）的点赞数量，根据userId集合的数量来确定
     */
    public long findEntityLikeCount(int entityType, int entityId) {
        String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
        return redisTemplate.opsForSet().size(entityLikeKey);
    }

    /**
     * 查询某个人对某实体的点赞状态
     * @param userId
     * @param entityType
     * @param entityId
     * @return
     */
    public int findEntityLikeStatus(int userId, int entityType, int entityId) {
        String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
        //检查当前的userId在不在value的set集合中，在的话说明点过赞了
        return redisTemplate.opsForSet().isMember(entityLikeKey, userId) ? 1 : 0;
    }
}
