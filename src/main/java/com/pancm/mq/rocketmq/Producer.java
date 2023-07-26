package com.pancm.mq.rocketmq;

import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.exception.RemotingException;

import java.nio.charset.StandardCharsets;

public class Producer {
    public static void main(String[] args) {
        DefaultMQProducer producer = new DefaultMQProducer("producer_group");
        producer.setNamesrvAddr("127.0.0.1:9876");
        try {
            producer.start();
        } catch (MQClientException e) {
            throw new RuntimeException(e);
        }
        //待发送消息
        Message topicA = new Message("topicA", "message".getBytes(StandardCharsets.UTF_8));

        //延迟消息
        // 非常简单 延迟级别
        // 1s 5s 10s 30s 1m 2m 3m 4m 5m 6m 7m 8m 9m 10m 20m 30m 1h 2h
        // 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18
        topicA.setDelayTimeLevel(2);

        long sendTime = System.currentTimeMillis();
        topicA.putUserProperty("sendTime", sendTime + "");

        try {
            SendResult sendResult = producer.send(topicA);
            System.out.printf(sendResult.toString());
        } catch (MQClientException | RemotingException | MQBrokerException | InterruptedException e) {
            throw new RuntimeException(e);
        }

    }
}
