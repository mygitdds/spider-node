package cn.spider.framework.controller.leader;

import cn.spider.framework.common.event.EventManager;
import cn.spider.framework.common.event.EventType;
import cn.spider.framework.common.event.data.FollowerDeathData;
import cn.spider.framework.common.event.data.NotifyLeaderCommissionData;
import cn.spider.framework.common.event.data.TranscriptChangeData;
import cn.spider.framework.common.utils.BrokerInfoUtil;
import cn.spider.framework.common.utils.ExceptionMessage;
import cn.spider.framework.container.sdk.interfaces.ContainerService;
import cn.spider.framework.controller.data.RegisterLeaderRequest;
import cn.spider.framework.controller.impl.LeaderHeartServiceImpl;
import cn.spider.framework.controller.sdk.interfaces.FollowerHeartService;
import cn.spider.framework.controller.sdk.interfaces.LeaderHeartService;
import cn.spider.framework.controller.sockt.BrokerClientInfo;
import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.SocketAddress;
import io.vertx.serviceproxy.ServiceBinder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @BelongsProject: spider-node
 * @BelongsPackage: cn.spider.framework.controller.leader
 * @Author: dengdongsheng
 * @CreateTime: 2023-04-19  19:04
 * @Description: leader的管理
 * @Version: 1.0
 */
@Slf4j
public class LeaderManager {

    private Map<String, BrokerClientInfo> followerMap;

    private NetServer netServer;

    private EventManager eventManager;

    private String brokerName;

    private String brokerIp;

    private Integer transcriptNum;

    private ServiceBinder binder;

    /**
     * 保存 follower
     */
    private Map<String, FollowerHeartService> followerHeartServiceMap;

    /**
     * 报错 副本的隐射关系
     */
    private Map<String, Set<String>> transcriptRelationMap;

    private Vertx vertx;


    public LeaderManager(EventManager eventManager, Vertx vertx) {
        this.followerMap = Maps.newHashMap();
        this.eventManager = eventManager;
        this.netServer = vertx.createNetServer();
        this.brokerName = BrokerInfoUtil.queryBrokerName(vertx);
        this.brokerIp = BrokerInfoUtil.queryBrokerIp(vertx);
        // 获取集群配置的副本数量
        this.transcriptNum = BrokerInfoUtil.queryTranscriptNum(vertx);
        this.binder = new ServiceBinder(vertx);
        this.followerHeartServiceMap = Maps.newHashMap();
        this.transcriptRelationMap = Maps.newHashMap();
        this.vertx = vertx;
    }

    public void init() {
        // 构建 leader的长连接
        registerBrokerClientInfo();
        // 通知 集群各个节点，我是leader
        notifyFollowerMyIsLeader();
        // 注册 -leader
        registerLeaderHeartConsumer();
    }

    private void registerLeaderHeartConsumer() {
        LeaderHeartService leaderHeartService = new LeaderHeartServiceImpl();
        String leaderHeartAddr = LeaderHeartService.ADDRESS;
        // 发布 LeaderHeartService的消费者
        this.binder.setAddress(leaderHeartAddr)
                .register(LeaderHeartService.class, leaderHeartService);
    }


    /**
     * 注册broker-follower
     */
    private void registerBrokerClientInfo() {
        netServer.connectHandler(socket -> {
            socket.handler(buffer -> {
                // 在这里应该解析报文，封装为协议对象，并找到响应的处理类，得到处理结果，并响应
                RegisterLeaderRequest request = JSON.parseObject(buffer.toString(), RegisterLeaderRequest.class);
                SocketAddress socketAddress = socket.remoteAddress();
                String ip = socketAddress.host();
                if (followerMap.containsKey(ip)) {
                    return;
                }
                BrokerClientInfo info = BrokerClientInfo.builder()
                        .brokerIp(request.getBrokerIp())
                        .brokerName(request.getBrokerName())
                        .socket(socket)
                        .virtuallyIp(ip)
                        .build();
                followerMap.put(info.getVirtuallyIp(), info);
                // 进行副本分配
                allocateReplicas(request.getBrokerName(), true);
                socket.write(Buffer.buffer("spider-leader-receive"));
                String followerHeartAddr = request.getBrokerName() + FollowerHeartService.ADDRESS;
                // 注册 生产者
                FollowerHeartService heartService = FollowerHeartService.createProxy(vertx, followerHeartAddr);
                this.followerHeartServiceMap.put(request.getBrokerIp(), heartService);
            });

            // 监听客户端的退出连接
            socket.closeHandler(close -> {
                // 退出，移除，client
                SocketAddress socketAddress = socket.remoteAddress();
                String ip = socketAddress.host();
                FollowerHeartService heartService = this.followerHeartServiceMap.get(ip);

                Future<Void> followerFuture = heartService.detection();
                followerFuture.onSuccess(suss -> {
                    // 说明只是链接断了，需要重新链接
                    Future<Void> connectFuture = heartService.reconnectLeader();
                    connectFuture.onFailure(fail -> {
                        log.error("follower ip {} 链接leader失败 {}", ip, ExceptionMessage.getStackTrace(fail));
                    });
                }).onFailure(fail -> {
                    BrokerClientInfo brokerClientInfo = followerMap.get(ip);
                    notifyAllFollowerLeave(brokerClientInfo);
                    followerMap.remove(ip);
                    // 重置副本分配
                    allocateReplicas(brokerClientInfo.getBrokerName(), false);
                });


            });
        });
    }

    /**
     * 通知集群其他存活节点，有节点挂了
     *
     * @param brokerClientInfo
     */
    public void notifyAllFollowerLeave(BrokerClientInfo brokerClientInfo) {
        FollowerDeathData followerDeathData = FollowerDeathData.builder()
                .brokerName(brokerClientInfo.getBrokerName())
                .build();
        // 跟该节点的follower进行通信-失败的情况下,进行集群通知
        eventManager.sendMessage(EventType.FOLLOWER_DEATH, followerDeathData);
    }

    /**
     * 副本分配- 副本分配的时机
     */
    public void allocateReplicas(String brokerName, Boolean isRegister) {
        if (this.transcriptNum == 0) {
            return;
        }
        // 注册
        if (isRegister) {
            // 说明是新注册的
            if (!this.transcriptRelationMap.containsKey(brokerName)) {
                this.transcriptRelationMap.put(brokerName, new HashSet<>(this.transcriptNum));
            }
            Set<String> transcriptKeys = this.transcriptRelationMap.keySet();
            for (String key : transcriptKeys) {
                Set<String> transcripts = this.transcriptRelationMap.get(key);
                int size = transcripts.size();
                if (size >= 2 || StringUtils.equals(key, brokerName) || transcripts.contains(brokerName)) {
                    continue;
                }
                transcripts.add(brokerName);
                break;
            }
            // 构建自己的副本
            Set<String> transcripts = this.transcriptRelationMap.get(brokerName);
            if (transcripts.size() >= 2) {
                return;
            }
            Collection<Set<String>> brokerNames = this.transcriptRelationMap.values();
            for (String key : transcriptKeys) {
                Integer appear = 0;
                for (Set<String> value : brokerNames) {
                    if (value.contains(key)) {
                        appear++;
                    }
                }
                if (appear < 2) {
                    transcripts.add(key);
                    if (transcripts.size() >= 2) {
                        return;
                    }
                }
            }
            sendTranscriptChange();
            // 重发事件
            return;
        }
        this.transcriptRelationMap.remove(brokerName);

        this.transcriptRelationMap.forEach((key, value) -> {
            Set<String> brokerNames = value;
            brokerNames.remove(brokerName);
        });
        sendTranscriptChange();
        // 重发时间
    }

    /**
     * 副本变化发送相关事件
     */
    private void sendTranscriptChange() {
        this.transcriptRelationMap.forEach((key, value) -> {
            eventManager.sendMessage(EventType.TRANSCRIPT_CHANGE, TranscriptChangeData.builder()
                    .transcript(value).brokerName(key)
                    .build());
        });
    }

    /**
     * 通知follower我是leader
     */
    public void notifyFollowerMyIsLeader() {
        NotifyLeaderCommissionData commissionData = NotifyLeaderCommissionData.builder()
                .brokerName(this.brokerName)
                .brokerIp(this.brokerIp)
                .build();
        // 通知follower-> 我是当前集群的leader
        eventManager.sendMessage(EventType.LEADER_GENERATE, commissionData);
    }

}
