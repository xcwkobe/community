package cn.xcw.community.mapper;

import cn.xcw.community.entity.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface CommentMapper {

    //根据目标类型和目标id查询所有的评论
    List<Comment> selectCommentsByEntity(@Param("entityType") int entityType,@Param("entityId") int entityId,
                                         @Param("offset") int offset,@Param("limit") int limit);

    //查询某个实体的评论数量
    int selectCountByEntity(@Param("entityType") int entityType,@Param("entityId") int entityId);

    int insertComment(Comment comment);

    Comment selectCommentById(int id);
}
