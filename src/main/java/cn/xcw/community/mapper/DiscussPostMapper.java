package cn.xcw.community.mapper;

import cn.xcw.community.entity.DiscussPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface DiscussPostMapper {

    //根据条件查询帖子，如果userId为0，就是查询首页的帖子
    List<DiscussPost> selectDiscussPost(@Param("userId")int userId,@Param("offset") int offset,
                                        @Param("limit") int limit,@Param("orderMode") int orderMode);

    //查到用户的发帖数
    int selectDiscussPostRows(@Param("userId") int userId);

    int insertDiscussPost(DiscussPost discussPost);


    DiscussPost selectDiscussPostById(int id);

    int updateCommentCount(@Param("id") int id,@Param("commentCount")int commentCount);

    //更新帖子的类型
    int updateType(@Param("id") int id,@Param("type") int type);

    int updateStatus(@Param("id") int id,@Param("status") int status);

    int updateScore(int id,double score);
}
