package com.heima.admin;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest
@RunWith(SpringRunner.class)
public class KafkaTest {


    @Autowired
    private KafkaTemplate kafkaTemplate;

    @Test
    public void test(){
        try {
            kafkaTemplate.send("topic.test","dgfs","hanqinyang...");
            System.out.println("======================消息发送了");
            Thread.sleep(50000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
