package cn.spider.framework.flow.consumer.system;
import cn.spider.framework.common.event.EventType;
import cn.spider.framework.common.event.data.FollowerDeathData;
import cn.spider.framework.flow.transcript.TranscriptManager;
import com.alibaba.fastjson.JSON;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageConsumer;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;

/**
 * @BelongsProject: spider-node
 * @BelongsPackage: cn.spider.framework.flow.consumer.system
 * @Author: dengdongsheng
 * @CreateTime: 2023-04-28  15:45
 * @Description: TODO
 * @Version: 消费对应节点挂了
 */
@Component
public class FollowerDeathHandler implements InitializingBean {

    @Resource
    private EventBus eventBus;

    @Resource
    private TranscriptManager transcriptManager;

    private EventType eventType = EventType.FOLLOWER_DEATH;

    public void registerConsumer(){
        MessageConsumer<String> consumer = eventBus.consumer(eventType.queryAddr());
        consumer.handler(message -> {
            FollowerDeathData followerDeathData = JSON.parseObject(message.body(),FollowerDeathData.class);
            transcriptManager.election(followerDeathData.getBrokerName());
        });
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.registerConsumer();
    }
}
