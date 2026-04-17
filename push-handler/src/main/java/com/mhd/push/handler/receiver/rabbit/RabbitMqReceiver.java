//package com.mhd.push.handler.receiver.rabbit;
//
//import com.alibaba.fastjson2.JSON;
//import com.mhd.push.common.domain.RecallTaskInfo;
//import com.mhd.push.common.domain.TaskInfo;
//import com.mhd.push.handler.receiver.MessageReceiver;
//import com.mhd.push.handler.receiver.service.ConsumeService;
//import com.mhd.push.support.constants.MessageQueuePipeline;
//import org.apache.commons.lang3.StringUtils;
//import org.springframework.amqp.core.ExchangeTypes;
//import org.springframework.amqp.core.Message;
//import org.springframework.amqp.rabbit.annotation.Exchange;
//import org.springframework.amqp.rabbit.annotation.Queue;
//import org.springframework.amqp.rabbit.annotation.QueueBinding;
//import org.springframework.amqp.rabbit.annotation.RabbitListener;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
//import org.springframework.stereotype.Component;
//
//import java.nio.charset.StandardCharsets;
//import java.util.List;
//
//
/// **
// * @author xzcawl
// * @date 23-04-21 10:53:32
// */
//@Component
//@ConditionalOnProperty(name = "austin.mq.pipeline", havingValue = MessageQueuePipeline.RABBIT_MQ)
//public class RabbitMqReceiver implements MessageReceiver {
//
//    @Autowired
//    private ConsumeService consumeService;
//
//    @RabbitListener(bindings = @QueueBinding(
//            value = @Queue(value = "${spring.rabbitmq.queues.send}", durable = "true"),
//            exchange = @Exchange(value = "${austin.rabbitmq.exchange.name}", type = ExchangeTypes.TOPIC),
//            key = "${austin.rabbitmq.routing.send}"
//    ))
//    public void send(Message message) {
//        byte[] body = message.getBody();
//        String messageContent = new String(body, StandardCharsets.UTF_8);
//        if (StringUtils.isBlank(messageContent)) {
//            return;
//        }
//        // 处理发送消息
//        List<TaskInfo> taskInfoLists = JSON.parseArray(messageContent, TaskInfo.class);
//        consumeService.consume2Send(taskInfoLists);
//    }
//
//    @RabbitListener(bindings = @QueueBinding(
//            value = @Queue(value = "${spring.rabbitmq.queues.recall}", durable = "true"),
//            exchange = @Exchange(value = "${austin.rabbitmq.exchange.name}", type = ExchangeTypes.TOPIC),
//            key = "${austin.rabbitmq.routing.recall}"
//    ))
//    public void recall(Message message) {
//        byte[] body = message.getBody();
//        String messageContent = new String(body, StandardCharsets.UTF_8);
//        if (StringUtils.isBlank(messageContent)) {
//            return;
//        }
//        // 处理撤回消息
//        RecallTaskInfo recallTaskInfo = JSON.parseObject(messageContent, RecallTaskInfo.class);
//        consumeService.consume2recall(recallTaskInfo);
//    }
//
//}
