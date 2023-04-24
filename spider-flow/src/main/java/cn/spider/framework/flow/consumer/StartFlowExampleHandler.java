package cn.spider.framework.flow.consumer;

import cn.spider.framework.common.event.EventType;
import io.vertx.core.MultiMap;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageConsumer;
import org.springframework.stereotype.Component;

/**
 * @BelongsProject: spider-node
 * @BelongsPackage: cn.spider.framework.flow.consumer
 * @Author: dengdongsheng
 * @CreateTime: 2023-04-24  16:00
 * @Description: TODO
 * @Version: 1.0
 */
@Component
public class StartFlowExampleHandler {

    private EventBus eventBus;

    private EventType eventType;

    public void registerConsumer(){
        MessageConsumer<String> consumer = eventBus.consumer(eventType.queryAddr());
        consumer.handler(message -> {
            MultiMap multiMap = message.headers();
            String brokerName = multiMap.get("brokerName");
            // 校验该本节点是否为 brokerName的功能follower

        });
    }

}
