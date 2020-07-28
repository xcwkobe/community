package cn.xcw.community.entity;

import lombok.Data;

import java.util.Date;

/**
 * @class: Comment
 * @author: 邢成伟
 * @description: TODO
 **/
@Data
public class Comment {

//    entity_type 评论的目标的类别 1：帖子 2: 评论 支持回复评论
//    entity_id 评论具体的目标的id
//    target_id 记录回复指向的人 (只会发生在回复中 判断target_id==0)
//    user_id 评论的作者

    private int id;
    private int userId;
    private int entityType;
    private int entityId;
    private int targetId;
    private String content;
    private int status;
    private Date CreateTime;
}
