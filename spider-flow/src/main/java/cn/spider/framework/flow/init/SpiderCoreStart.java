package cn.spider.framework.flow.init;

import cn.spider.framework.common.role.BrokerRole;
import cn.spider.framework.common.utils.BrokerInfoUtil;
import cn.spider.framework.common.utils.LeaderUtil;
import cn.spider.framework.container.sdk.interfaces.BusinessService;
import cn.spider.framework.container.sdk.interfaces.ContainerService;
import cn.spider.framework.container.sdk.interfaces.FlowService;
import cn.spider.framework.container.sdk.interfaces.LeaderService;
import com.google.common.collect.Lists;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceBinder;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @BelongsProject: spider-node
 * @BelongsPackage: cn.spider.framework.flow.init
 * @Author: dengdongsheng
 * @CreateTime: 2023-04-21  14:54
 * @Description: 初始化core
 * @Version: 1.0
 */
@Component
public class SpiderCoreStart {

    private ApplicationContext applicationContext;

    private BrokerRole brokerRole;

    private ServiceBinder binder;

    private List<MessageConsumer<JsonObject>> containerConsumers;

    private Vertx vertx;

    public SpiderCoreStart(Vertx vertx, ApplicationContext applicationContext) {
        this.vertx = vertx;
        this.binder = new ServiceBinder(vertx);
        this.brokerRole = LeaderUtil.queryBrokerRole(vertx);
        this.applicationContext = applicationContext;
        this.containerConsumers = Lists.newArrayList();
    }

    public void upgrade() {
        this.brokerRole = LeaderUtil.queryBrokerRole(vertx);
        startComponentByLeader();
    }

    public void startComponentByLeader() {
        // leader才会注册以下服务
        if (!this.brokerRole.equals(BrokerRole.LEADER)) {
            return;
        }
        ContainerService containerService = applicationContext.getBean(ContainerService.class);

        String containerAddr = BrokerRole.LEADER.name() + ContainerService.ADDRESS;
        MessageConsumer<JsonObject> containerConsumer = this.binder
                .setAddress(containerAddr)
                .register(ContainerService.class, containerService);
        containerConsumers.add(containerConsumer);

        BusinessService businessService = applicationContext.getBean(BusinessService.class);

        String businessAddr = BrokerRole.LEADER.name() + BusinessService.ADDRESS;

        MessageConsumer<JsonObject> businessConsumer = this.binder
                .setAddress(businessAddr)
                .register(BusinessService.class, businessService);
        containerConsumers.add(businessConsumer);
    }

    public void necessaryComponent() {
        FlowService flowService = applicationContext.getBean(FlowService.class);
        MessageConsumer<JsonObject> flowConsumer = this.binder
                .setAddress(FlowService.ADDRESS)
                .register(FlowService.class, flowService);
        containerConsumers.add(flowConsumer);

        LeaderService leaderService = applicationContext.getBean(LeaderService.class);
        String leaderAddr = BrokerInfoUtil.queryBrokerName(vertx) + LeaderService.ADDRESS;
        MessageConsumer<JsonObject> leaderConsumer = this.binder
                .setAddress(leaderAddr)
                .register(LeaderService.class, leaderService);
        containerConsumers.add(leaderConsumer);
    }

    /**
     * 注销消费者
     */
    public void unregister() {
        for (MessageConsumer<JsonObject> consumer : containerConsumers) {
            this.binder.unregister(consumer);
        }
    }
}
