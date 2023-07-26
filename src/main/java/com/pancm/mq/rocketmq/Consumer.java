package com.pancm.mq.rocketmq;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.MessageExt;

import java.util.List;

public class Consumer {
    public static void main(String[] args) {
        DefaultMQPushConsumer mqConsumer = new
                DefaultMQPushConsumer("consumer_group");
        mqConsumer.setNamesrvAddr("localhost:9876");

        // 设置消息监听器
        mqConsumer.registerMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> list, ConsumeConcurrentlyContext consumeConcurrentlyContext) {
                MessageExt messageExt = list.get(0);
                //一定程度 反映消息传输时间
                long endTime = System.currentTimeMillis();
                String sendTime = messageExt.getProperty("sendTime");
                System.out.printf("time span:" + (endTime - Long.parseLong(sendTime)));

                byte[] body = messageExt.getBody();
                System.out.printf(String.valueOf(body.length));
                System.out.printf(messageExt.getMsgId());
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });
        //订阅主题
        try {
            mqConsumer.subscribe("topicA", "*");
        } catch (MQClientException e) {
            throw new RuntimeException(e);
        }
        try {
            mqConsumer.start();
        } catch (MQClientException e) {
            throw new RuntimeException(e);
        }
    }
}