package cn.spider.framework.controller.follower;

import cn.spider.framework.common.config.Constant;
import cn.spider.framework.common.event.EventType;
import cn.spider.framework.common.event.data.NotifyLeaderCommissionData;
import cn.spider.framework.common.role.BrokerRole;
import cn.spider.framework.common.utils.BrokerInfoUtil;
import cn.spider.framework.common.utils.ExceptionMessage;
import cn.spider.framework.common.utils.SpringUtil;
import cn.spider.framework.container.sdk.interfaces.LeaderService;
import cn.spider.framework.controller.data.RegisterLeaderRequest;
import cn.spider.framework.controller.impl.FollowerHeartServiceImpl;
import cn.spider.framework.controller.leader.Leader;
import cn.spider.framework.controller.leader.LeaderManager;
import cn.spider.framework.controller.sdk.interfaces.FollowerHeartService;
import com.alibaba.fastjson.JSON;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.shareddata.Lock;
import io.vertx.serviceproxy.ServiceBinder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * @BelongsProject: spider-node
 * @BelongsPackage: cn.spider.framework.controller.follower
 * @Author: dengdongsheng
 * @CreateTime: 2023-04-19  19:04
 * @Description: 集群内-追随者管理
 * @Version: 1.0
 */
@Slf4j
public class FollowerManager {

    private Leader leader;

    private NetClient client;

    private String followerName;

    private String followerIp;

    private EventBus eventBus;

    // 监听leader创建事件
    private MessageConsumer<String> consumerLeaderCreate;

    private RedisTemplate<String, String> redisTemplate;

    private LeaderService leaderService;

    private Vertx vertx;

    private ServiceBinder binder;

    MessageConsumer<JsonObject> followerHeartConsumer;

    public FollowerManager(Vertx vertx, RedisTemplate<String, String> redisTemplate) {
        NetClientOptions options = new NetClientOptions()
                .setLogActivity(true)
                .setConnectTimeout(10000);
        this.eventBus = vertx.eventBus();
        this.client = vertx.createNetClient(options);
        this.followerIp = BrokerInfoUtil.queryBrokerIp(vertx);
        this.followerName = BrokerInfoUtil.queryBrokerName(vertx);
        this.vertx = vertx;
        this.redisTemplate = redisTemplate;
        String leaderServiceAddr = BrokerRole.LEADER.name() + LeaderService.ADDRESS;
        this.leaderService = LeaderService.createProxy(vertx, leaderServiceAddr);
        this.binder = new ServiceBinder(vertx);
    }

    public void init() {
        informLeaderConsumer();
        // 注册
        String followerHeartAddr = this.followerName + FollowerHeartService.ADDRESS;
        FollowerHeartService followerHeartService = new FollowerHeartServiceImpl();
        this.followerHeartConsumer = this.binder.setAddress(followerHeartAddr)
                .register(FollowerHeartService.class, followerHeartService);

    }

    public void stop() {
        // 取消监听
        this.consumerLeaderCreate.unregister();
        this.leader = Leader.builder()
                .brokerName(this.followerName)
                .brokerIp(this.followerIp)
                .build();
        // 卸载
        this.followerHeartConsumer.unregister();
    }

    /**
     * 注册监听-leader创建的事件
     */
    public void informLeaderConsumer() {
        this.consumerLeaderCreate = eventBus.consumer(EventType.LEADER_GENERATE.queryAddr());
        this.consumerLeaderCreate.handler(message -> {
            NotifyLeaderCommissionData commissionData = JSON.parseObject(message.body(), NotifyLeaderCommissionData.class);
            this.leader = Leader.builder()
                    .brokerIp(commissionData.getBrokerIp())
                    .brokerName(commissionData.getBrokerName())
                    .build();
            // 告知不需要去竞争了，已经有leader了
            // 向leader注册
            Future<Void> connectFuture = connect();
            connectFuture
                    .onSuccess(suss -> {
                        // 注册关闭handler
                        monitorSocket();
                    })
                    .onFailure(fail -> {
                        log.error("follower {},leader{}链接失败请检查网络 异常原因 {}", this.followerName, this.leader.getBrokerName(), ExceptionMessage.getStackTrace(fail));
                    });
        });
    }

    // 向leader发起链接事件
    public Future<Void> connect() {
        Promise<Void> promise = Promise.promise();
        // 跟leader通信
        this.client.connect(9063, leader.getBrokerIp(), res -> {
            if (res.succeeded()) {
                leader.setSocket(res.result());
                // 向leader汇报自己的信息
                RegisterLeaderRequest request = RegisterLeaderRequest.builder()
                        .brokerIp(followerIp)
                        .brokerName(followerName)
                        .build();
                JsonObject followerInfo = JsonObject.mapFrom(request);
                this.leader.getSocket().write(Buffer.buffer(followerInfo.toString()));
                promise.complete();
            } else {
                System.out.println("Failed to connect: " + res.cause().getMessage());
                promise.fail(res.cause());
            }
        });
        return promise.future();
    }

    /**
     * 监听关闭-说明leader断开
     */
    public void monitorSocket() {
        // 监听客户端的退出连接
        leader.getSocket().closeHandler(close -> {
            // leader已经断开
            // 获取vertx集群的分布式锁
            this.vertx.sharedData().getLock(Constant.CAMPAIGN_LEADER)
                    .onSuccess(suss -> {
                        Lock lock = suss;
                        String brokerName = this.redisTemplate.opsForValue().get(Constant.LEADER_CONFIG_KEY);
                        if (StringUtils.isEmpty(brokerName) || StringUtils.equals(brokerName, leader.getBrokerName())) {
                            // 放弃当前为follower的角色
                            Future<Void> upgrade = leaderService.upgrade();
                            upgrade.onSuccess(upgradeSuss -> {
                                // 直接设置- 晋升为leader
                                this.redisTemplate.opsForValue().set(Constant.LEADER_CONFIG_KEY, this.followerName);
                                // 启动当前节点的leader角色
                                LeaderManager leaderManager = SpringUtil.getBean(LeaderManager.class);
                                leaderManager.init();
                                // 关闭follower信息
                                this.stop();
                                // 释放锁
                                lock.release();
                            }).onFailure(fail -> {
                                lock.release();
                                log.error("重置leader-fail {}", ExceptionMessage.getStackTrace(fail));
                            });

                        }
                    }).onFailure(fail -> {
                        log.error("锁获取失败 {}", ExceptionMessage.getStackTrace(fail));
                    });

        });
    }

}
