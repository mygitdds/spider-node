package cn.spider.framework.flow.consumer.system;
import cn.spider.framework.common.config.Constant;
import cn.spider.framework.common.event.EventType;
import cn.spider.framework.common.event.data.DestroyBpmnData;
import cn.spider.framework.common.utils.BrokerInfoUtil;
import cn.spider.framework.flow.resource.factory.StartEventFactory;
import com.alibaba.fastjson.JSON;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageConsumer;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;

/**
 * @BelongsProject: spider-node
 * @BelongsPackage: cn.spider.framework.flow.consumer.system
 * @Author: dengdongsheng
 * @CreateTime: 2023-05-08  14:42
 * @Description: TODO
 * @Version: 1.0
 */
@Component
public class AsyncDestroyBpmnHandler implements InitializingBean {
    @Resource
    private EventBus eventBus;

    @Resource
    private Vertx vertx;

    @Resource
    private StartEventFactory startEventFactory;

    private EventType eventType = EventType.DESTROY_BPMN;

    private String localBrokerName;

    public void registerConsumer() {
        MessageConsumer<String> consumer = eventBus.consumer(eventType.queryAddr());
        consumer.handler(message -> {
            MultiMap multiMap = message.headers();
            String brokerName = multiMap.get(Constant.BROKER_NAME);
            // 自身的事件不进行消费
            if(StringUtils.equals(brokerName,localBrokerName)){
                return;
            }
            DestroyBpmnData destroyBpmnData = JSON.parseObject(message.body(),DestroyBpmnData.class);
            startEventFactory.destroyBpmn(destroyBpmnData.getBpmnName());
        });
    }


    @Override
    public void afterPropertiesSet() throws Exception {
        registerConsumer();
        this.localBrokerName = BrokerInfoUtil.queryBrokerName(vertx);
    }
}
