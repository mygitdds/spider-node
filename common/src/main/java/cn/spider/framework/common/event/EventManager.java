package cn.spider.framework.common.event;

import cn.spider.framework.common.config.Constant;
import cn.spider.framework.common.event.data.EventData;
import cn.spider.framework.common.utils.BrokerInfoUtil;
import io.vertx.core.Vertx;
import io.vertx.core.WorkerExecutor;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * @BelongsProject: spider-node
 * @BelongsPackage: cn.spider.framework.common.event
 * @Author: dengdongsheng
 * @CreateTime: 2023-04-19  13:14
 * @Description: 事件管理
 * @Version: 1.0
 */
@Slf4j
public class EventManager {
    private EventBus eventBus;

    private String brokerName;


    public EventManager(Vertx vertx) {
        this.eventBus = vertx.eventBus();
        this.brokerName = BrokerInfoUtil.queryBrokerName(vertx);
    }

    /**
     * 发送消息-- 消费者
     *
     * @param eventType
     * @param eventData
     */
    public void sendMessage(EventType eventType, EventData eventData) {
        DeliveryOptions options = new DeliveryOptions();
        options.addHeader(Constant.BROKER_NAME, brokerName);
        options.addHeader(Constant.EVENT_NAME, eventType.getName());
        eventData.setTime(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        // 告知 谁是消费者
        JsonObject param = JsonObject.mapFrom(eventData);
        String paramString = param.toString();
        String addr = eventType.queryAddr();
        eventBus.publish(addr, paramString, options);
    }
}
