package cn.xcw.community.service;

import cn.xcw.community.entity.DiscussPost;
import cn.xcw.community.mapper.DiscussPostMapper;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @class: DiscussPostService
 * @author: 邢成伟
 * @description: TODO
 **/
@Service
public class DiscussPostService {

    private static final Logger logger= LoggerFactory.getLogger(DiscussPostService.class);

    @Autowired
    private DiscussPostMapper discussPostMapper;

    //caffine的核心接口：Cache LoadingCache ayncLoadingCache
    //帖子列表的缓存
    private LoadingCache<String,List<DiscussPost>> postListCache;

    //缓存帖子的总数
    private LoadingCache<Integer,Integer> postRowsCache;

    @Value("${caffeine.posts.max-size}")
    private int maxSize;

    @Value("${caffeine.posts.expire-seconds}")
    private int expireSeconds;

    /**
     * 被@PostConstruct修饰的方法会在服务器加载Servlet的时候运行，
     * 并且只会被服务器调用一次，类似于Serclet的inti()方法。
     * 被@PostConstruct修饰的方法会在构造函数之后，init()方法之前运行
     */
    @PostConstruct
    public void init() {
        // 初始化帖子列表本地缓存
        postListCache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireSeconds, TimeUnit.SECONDS)
                .build(new CacheLoader<String, List<DiscussPost>>() {
                    @Nullable
                    @Override
                    public List<DiscussPost> load(@NonNull String key) throws Exception {
                        if (key == null || key.length() == 0) {
                            throw new IllegalArgumentException("参数错误!");
                        }

                        String[] params = key.split(":");
                        if (params == null || params.length != 2) {
                            throw new IllegalArgumentException("参数错误!");
                        }

                        int offset = Integer.valueOf(params[0]);
                        int limit = Integer.valueOf(params[1]);

                        // 二级缓存: Redis -> mysql

                        logger.debug("load post list from DB.");
                        return discussPostMapper.selectDiscussPost(0, offset, limit, 1);
                    }
                });
        // 初始化帖子总数缓存
        postRowsCache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireSeconds, TimeUnit.SECONDS)
                .build(new CacheLoader<Integer, Integer>() {
                    @Nullable
                    @Override
                    public Integer load(@NonNull Integer key) throws Exception {
                        logger.debug("load post rows from DB.");
                        return discussPostMapper.selectDiscussPostRows(key);
                    }
                });
    }

    /**
     * 根据用户id查到用户的帖子总数
     * @param userId
     * @return
     */
    public int findDiscussPostRows(int userId) {
        //先从缓存中找到帖子总数
        if(userId==0){
            return postRowsCache.get(userId);
        }
        logger.debug("load post rows form db");
        return discussPostMapper.selectDiscussPostRows(userId);
    }

    /**
     * 根据条件查询帖子集合
     * @param userId
     * @param offset
     * @param limit
     * @param orderMode
     * @return
     */
    public List<DiscussPost> findDiscussPosts(int userId, int offset, int limit, int orderMode) {
        if(userId==0&&orderMode==1){
            return postListCache.get(offset+":"+limit);
        }
        logger.debug("load post list form db");
        return discussPostMapper.selectDiscussPost(userId, offset, limit,orderMode);
    }

    public int addDiscussPost(DiscussPost post) {
        if(post==null){
            throw new IllegalArgumentException("参数不能为空");
        }
        //防止加入html的内容
        post.setTitle(HtmlUtils.htmlEscape(post.getTitle()));
        post.setContent(HtmlUtils.htmlEscape(post.getContent()));

        //过滤敏感词
//        post.setTitle(sensitiveFilter.filter(post.getTitle()));
//        post.setContent(sensitiveFilter.filter(post.getContent()));
        return discussPostMapper.insertDiscussPost(post);
    }

    public DiscussPost findDiscussPostById(int discussPostId) {
        return discussPostMapper.selectDiscussPostById(discussPostId);
    }

    public int updateType(int id, int type) {
        return discussPostMapper.updateType(id,type);
    }

    public int updateStatus(int id, int status) {
        return discussPostMapper.updateStatus(id,status);
    }

    //更新帖子的评论数量
    public int updateCommentCount(int id,int commentCount){
        return discussPostMapper.updateCommentCount(id,commentCount);
    }
}
