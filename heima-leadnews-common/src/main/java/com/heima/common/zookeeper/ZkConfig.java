package com.heima.common.zookeeper;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Data
@Configuration
@PropertySource("classpath:zookeeper.properties")
@ConfigurationProperties(prefix = "zk")
public class ZkConfig {
    private String host;
    private String sequencePath;

    @Bean
    public ZookeeperClient zookeeperClient(){
        return new ZookeeperClient(this.sequencePath,this.host);
    }
}
