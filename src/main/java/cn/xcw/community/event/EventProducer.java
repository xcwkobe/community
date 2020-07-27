package cn.xcw.community.event;

import cn.xcw.community.entity.Event;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * @class: EvenProducer
 * @author: 邢成伟
 * @description: kafka消息发布
 **/

@Component
public class EventProducer {

    @Autowired
    private KafkaTemplate kafkaTemplate;

    //处理事件(发送事件)
    public void fireEvent(Event event){
        //将事件发布到指定的主题，事件event就是数据，转换为json
        kafkaTemplate.send(event.getTopic(), JSONObject.toJSONString(event));
    }
}
