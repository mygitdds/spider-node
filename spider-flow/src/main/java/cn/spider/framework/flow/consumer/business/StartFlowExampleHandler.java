package cn.spider.framework.flow.consumer.business;

import cn.spider.framework.common.config.Constant;
import cn.spider.framework.common.event.EventType;
import cn.spider.framework.common.event.data.StartFlowExampleEventData;
import cn.spider.framework.container.sdk.data.StartFlowRequest;
import cn.spider.framework.flow.engine.StoryEngine;
import cn.spider.framework.flow.engine.example.enums.FlowExampleRole;
import cn.spider.framework.flow.engine.facade.ReqBuilder;
import cn.spider.framework.flow.engine.facade.StoryRequest;
import cn.spider.framework.flow.load.loader.ClassLoaderManager;
import cn.spider.framework.flow.transcript.TranscriptManager;
import com.alibaba.fastjson.JSON;
import io.vertx.core.MultiMap;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageConsumer;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;

/**
 * @BelongsProject: spider-node
 * @BelongsPackage: cn.spider.framework.flow.consumer
 * @Author: dengdongsheng
 * @CreateTime: 2023-04-24  16:00
 * @Description: TODO
 * @Version: 1.0
 */
@Component
public class StartFlowExampleHandler implements InitializingBean {

    @Resource
    private EventBus eventBus;

    @Resource
    private TranscriptManager transcriptManager;

    @Resource
    private ClassLoaderManager classLoaderManager;

    @Resource
    private StoryEngine storyEngine;

    private EventType eventType = EventType.START_FLOW_EXAMPLE;


    public void registerConsumer() {
        MessageConsumer<String> consumer = eventBus.consumer(eventType.queryAddr());
        consumer.handler(message -> {
            MultiMap multiMap = message.headers();
            String brokerName = multiMap.get(Constant.BROKER_NAME);
            // 校验该本节点是否为 brokerName的功能follower
            if (!transcriptManager.checkIsTranscript(brokerName)) {
                return;
            }

            StartFlowExampleEventData eventData = JSON.parseObject(message.body(), StartFlowExampleEventData.class);
            StartFlowRequest request = eventData.getRequestParam().mapTo(StartFlowRequest.class);
            StoryRequest<Object> storyRequest = ReqBuilder.returnType(Object.class)
                    .startId(eventData.getStartId())
                    .functionName(eventData.getFunctionName())
                    .startFlowRequest(request)
                    // 默认给leader
                    .flowExampleRole(FlowExampleRole.FOLLOWER)
                    .functionId(eventData.getFunctionId())
                    .requestId(eventData.getRequestId())
                    .request(request.getRequest(classLoaderManager.queryClassLoader(eventData.getRequestClassType())))
                    .build();
            storyEngine.getFlowExampleManager().registerFollowerExample(storyRequest, brokerName);
        });
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.registerConsumer();
    }
}
