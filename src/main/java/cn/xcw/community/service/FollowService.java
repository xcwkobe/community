package cn.xcw.community.service;

import cn.xcw.community.entity.User;
import cn.xcw.community.util.CommunityConstant;
import cn.xcw.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * @class: FollowService
 * @author: 邢成伟
 * @description: TODO
 **/
@Service
public class FollowService implements CommunityConstant{

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private UserService userService;

    /**
     * 查询用户（userId）关注的实体的数量
     * @param userId
     * @param entityType
     * @return
     */
    public long findFolloweeCount(int userId, int entityType) {
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
        return redisTemplate.opsForZSet().zCard(followeeKey);
    }

    /**
     * 查询实体的粉丝数量
     * @param entityType user
     * @param entityId userId
     * @return
     */
    public long findFollowerCount(int entityType, int entityId) {
        String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);
        return redisTemplate.opsForZSet().zCard(followerKey);
    }

    /**
     * 查询当前用户（userId）是否已经关注了该实体
     * @param userId
     * @param entityType
     * @param entityId
     * @return
     */
    public boolean hasFollowed(int userId, int entityType, int entityId) {
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
        return redisTemplate.opsForZSet().score(followeeKey, entityId) != null;
    }

    /**
     * 当前用户（userId）关注另一个实体（entityType，entityId）
     * @param userId
     * @param entityType 实体可以是用户，帖子，话题
     * @param entityId
     */
    public void follow(int userId, int entityType, int entityId) {
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                //获得某个用户关注的实体
                String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
                //某个实体（用户）的粉丝
                String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);
                //redis事务开始
                operations.multi();
                //这个分数score就是关注的时间
                operations.opsForZSet().add(followeeKey, entityId, System.currentTimeMillis());
                operations.opsForZSet().add(followerKey, userId, System.currentTimeMillis());
                //执行所有事务
                return operations.exec();
            }
        });
    }

    /**
     * 某个用户取消对某个实体的关注
     * @param userId
     * @param entityType
     * @param entityId
     */
    public void unfollow(int userId, int entityType, int entityId) {
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
                String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);
                operations.multi();
                operations.opsForZSet().remove(followeeKey, entityId);
                operations.opsForZSet().remove(followerKey, userId);
                return operations.exec();
            }
        });
    }

    /**
     * 查询这个用户（userId）关注的人
     * @param userId
     * @param offset
     * @param limit
     * @return
     */
    public List<Map<String, Object>> findFollowees(int userId, int offset, int limit) {
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, ENTITY_TYPE_USER);
        //索引倒序排列指定区间元素reverseRange(K key, long start, long end)
        //获得关注的用户的id集合
        Set<Integer> targetIds = redisTemplate.opsForZSet().reverseRange(followeeKey, offset, offset + limit - 1);
        if (targetIds == null) {
            return null;
        }
        List<Map<String, Object>> list = new ArrayList<>();
        for (Integer targetId : targetIds) {
            Map<String, Object> map = new HashMap<>();
            User user = userService.findUserById(targetId);
            map.put("user", user);
            // 获取元素的分值，就是关注的时间
            Double score = redisTemplate.opsForZSet().score(followeeKey, targetId);
            map.put("followTime", new Date(score.longValue()));
            list.add(map);
        }
        return list;
    }

    /**
     * 查询关注该用户（userId）的人，粉丝
     * @param userId
     * @param offset
     * @param limit
     * @return
     */
    public List<Map<String, Object>> findFollowers(int userId, int offset, int limit) {
        String followerKey = RedisKeyUtil.getFollowerKey(ENTITY_TYPE_USER, userId);
        Set<Integer> targetIds = redisTemplate.opsForZSet().reverseRange(followerKey, offset, offset + limit - 1);
        if (targetIds == null) {
            return null;
        }
        List<Map<String, Object>> list = new ArrayList<>();
        for (Integer targetId : targetIds) {
            Map<String, Object> map = new HashMap<>();
            User user = userService.findUserById(targetId);
            map.put("user", user);
            Double score = redisTemplate.opsForZSet().score(followerKey, targetId);
            map.put("followTime", new Date(score.longValue()));
            list.add(map);
        }
        return list;
    }
}
