package cn.spider.framework.flow.consumer.business;

import cn.spider.framework.common.event.EventType;
import cn.spider.framework.flow.engine.StoryEngine;
import cn.spider.framework.flow.transcript.TranscriptManager;
import io.vertx.core.eventbus.EventBus;

import javax.annotation.Resource;

/**
 * @BelongsProject: spider-node
 * @BelongsPackage: cn.spider.framework.flow.consumer.business
 * @Author: dengdongsheng
 * @CreateTime: 2023-04-25  17:42
 * @Description: TODO
 * @Version: 1.0
 */
public class EndElementExampleHandler {
    @Resource
    private EventBus eventBus;

    @Resource
    private TranscriptManager transcriptManager;

    @Resource
    private StoryEngine storyEngine;

    private EventType eventType = EventType.ELEMENT_START;
}
