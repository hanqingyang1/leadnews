package com.heima.common.zookeeper;

import com.google.common.collect.Maps;
import com.heima.common.zookeeper.sequence.ZkSequence;
import com.heima.common.zookeeper.sequence.ZkSequenceEnum;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;

import javax.annotation.PostConstruct;
import java.util.Map;

@Slf4j
@Data
public class ZookeeperClient {

    private String host;

    private String sequencePath;
    //创建连接实例
    CuratorFramework client = null;

    // 重试休眠时间
    private final int SLEEP_TIME_MS = 1000;
    // 最大重试1000次
    private final int MAX_RETRIES = 1000;
    //会话超时时间
    private final int SESSION_TIMEOUT = 30 * 1000;
    //连接超时时间
    private final int CONNECTION_TIMEOUT = 3 * 1000;

    //创建序列化集合
    // 序列化集合
    private Map<String, ZkSequence> zkSequence = Maps.newConcurrentMap();

    public ZookeeperClient(String sequencePath, String host){
        this.host = host;
        this.sequencePath = sequencePath;
    }


    /**
     *初始化连接实例
     */
    @PostConstruct
    public void init(){
       this.client =  CuratorFrameworkFactory.builder()
                .connectString(host)
                .connectionTimeoutMs(CONNECTION_TIMEOUT)
                .sessionTimeoutMs(SESSION_TIMEOUT)
                .retryPolicy(new ExponentialBackoffRetry(SLEEP_TIME_MS,MAX_RETRIES))
                .build();
        this.client.start();
        this.initZkSequence();

    }

    /**
     * 初始化
     */
    public void initZkSequence(){
        ZkSequenceEnum[] list = ZkSequenceEnum.values();
        for (int i = 0; i < list.length; i++) {
            String name = list[i].name();
            String path = this.sequencePath+name;
            ZkSequence seq = new ZkSequence(this.client, path);
            zkSequence.put(name,seq);
        }
    }


    public Long sequence(ZkSequenceEnum name){
        try {
            ZkSequence seq = this.zkSequence.get(name.name());
            if(seq != null){
                return seq.sequence();
            }
        }catch (Exception e){
            log.error("获取[{}]Sequence错误:{}",name,e);
            e.printStackTrace();
        }
        return null;
    }
}
