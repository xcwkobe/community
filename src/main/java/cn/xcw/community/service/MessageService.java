package cn.xcw.community.service;

import cn.xcw.community.entity.Message;
import cn.xcw.community.mapper.MessageMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.util.List;

/**
 * @class: MessageService
 * @author: 邢成伟
 * @description: TODO
 **/

@Service
public class MessageService {

    @Autowired
    private MessageMapper messageMapper;

//    @Autowired
//    private SensitiveFilter sensitiveFilter;

    /**
     * 查询用户的会话数量
     * @param userId
     * @return
     */
    public int findConversationCount(int userId){
        return messageMapper.selectConversationCount(userId);
    }

    /**
     * 查询当前用户的会话列表，针对每次会话值返回最新的私信
     * @param userId
     * @param offset
     * @param limit
     * @return
     */
    public List<Message> findConverations(int userId, int offset, int limit){
        return messageMapper.selectConversations(userId,offset,limit);
    }

    /**
     * 查询某个回话所包含的私信数量
     * @param conversationId
     * @return
     */
    public int findLetterCount(String conversationId){
        return messageMapper.selectLetterCount(conversationId);
    }

    /**
     * 用户未读私信的数量
     * @param userId
     * @param conversationId
     * @return
     */
    public int findLetterUnreadCount(int userId,String conversationId){
        return messageMapper.selectLetterUnreadCount(userId,conversationId);
    }

    /**
     * 查询未读的通知（或主题）的数量
     * @param userId
     * @param topic
     * @return
     */
    public int findNoticeUnreadCount(int userId,String topic){
        return messageMapper.selectNoticeUnreadCount(userId,topic);
    }

    /**
     * 查询某个回话所包含的私信列表
     * @param conversationId
     * @param offset
     * @param limit
     * @return
     */
    public List<Message> findLetters(String conversationId,int offset,int limit){
        return messageMapper.selectLetters(conversationId,offset,limit);
    }

    /**
     * 更新多条消息为已读，状态为1
     * @param ids
     * @return
     */
    public int readMessage(List<Integer> ids){
        return messageMapper.updateStatus(ids,1);
    }

    public int addMessage(Message message){
        message.setContent(HtmlUtils.htmlEscape(message.getContent()));
//        message.setContent(sensitiveFilter.filter(message.getContent()));
        return messageMapper.insertMessage(message);
    }

    /**
     * 查询某个主题下的最新的通知
     * @param userId
     * @param topic 有评论类，关注类，点赞类
     * @return
     */
    public Message findLatestNotice(int userId,String topic){
        return messageMapper.selectLatestNotice(userId,topic);
    }

    /**
     * 查询某个主题所包含的通知数量
     * @param userId
     * @param topic
     * @return
     */
    public int findNoticeCount(int userId,String topic){
        return messageMapper.selectNoticeCount(userId,topic);
    }

    /**
     * 查寻某个主题所包含的通知的列表
     * @param userId
     * @param topic
     * @param offset
     * @param limit
     * @return
     */
    public List<Message> findNotices(int userId,String topic,int offset,int limit){
        return messageMapper.selectNotices(userId,topic,offset,limit);
    }
}
