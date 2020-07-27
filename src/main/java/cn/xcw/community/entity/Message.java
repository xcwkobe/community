package cn.xcw.community.entity;

import lombok.Data;

import java.util.Date;

/**
 * @class: Message
 * @author: 邢成伟
 * @description: 消息实体，包括私信和通知，支持两个用户发送消息
 **/
@Data
public class Message {

//    from_id 112 其中id = 1代表的是：系统通知
//    to_id 111
//    conversion_id 111_112(id小的在前)111与112 之间的会话
//    status 0：未读    1:已读     2：删除

    private int id;
    private int fromId;
    private int toId;
    //唯一的会话id，以用户id小的开头，id大的结尾比如12_111
    private String conversationId;
    private String content;
    private int status;
    private Date createTime;
}
