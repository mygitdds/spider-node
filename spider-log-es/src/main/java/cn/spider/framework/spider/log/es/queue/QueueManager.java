package cn.spider.framework.spider.log.es.queue;

import cn.spider.framework.common.utils.ExceptionMessage;
import cn.spider.framework.spider.log.es.client.EsIndexTypeId;
import cn.spider.framework.spider.log.es.domain.ElementExampleLog;
import cn.spider.framework.spider.log.es.domain.SpiderFlowElementExampleLog;
import cn.spider.framework.spider.log.es.domain.SpiderFlowExampleLog;
import cn.spider.framework.spider.log.es.service.SpiderFlowElementExampleService;
import cn.spider.framework.spider.log.es.service.SpiderFlowExampleLogService;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @BelongsProject: spider-node
 * @BelongsPackage: cn.spider.framework.spider.log.es.queue
 * @Author: dengdongsheng
 * @CreateTime: 2023-04-18  21:01
 * @Description: 队列的管理
 * @Version: 1.0
 */
@Slf4j
public class QueueManager {
    //队列
    private BlockingQueue<String> flowExampleQueue;

    private Vertx vertx;

    // 流程实例的service
    private SpiderFlowElementExampleService spiderFlowElementExampleService;

    private SpiderFlowExampleLogService exampleLogService;

    public QueueManager(Vertx vertx,
                        SpiderFlowElementExampleService spiderFlowElementExampleService,
                        SpiderFlowExampleLogService exampleLogService) {
        this.vertx = vertx;
        this.flowExampleQueue = new ArrayBlockingQueue<>(15000, true);
        this.exampleLogService = exampleLogService;
        this.spiderFlowElementExampleService = spiderFlowElementExampleService;
        registerTimer();
    }

    public void insertQueue(String param) {
        flowExampleQueue.offer(param);
    }

    public void consumerByBatch() {
        try {
            if(flowExampleQueue.size() == 0){
                return;
            }
            List<String> list = new ArrayList<>();
            Queues.drain(flowExampleQueue, list, 500, 2, TimeUnit.SECONDS);
            if (CollectionUtils.isEmpty(list)) {
                return;
            }
            List<EsIndexTypeId> elementExampleLogs = Lists.newArrayList();
            List<EsIndexTypeId> flowExampleLogs = Lists.newArrayList();
            for (String value : list) {
                JsonObject example = new JsonObject(value);
                ElementExampleLog elementExampleLog = example.mapTo(ElementExampleLog.class);
                switch (elementExampleLog.getExampleType()) {
                    case FLOW:
                        flowExampleLogs.add((SpiderFlowExampleLog) elementExampleLog.getExampleLog());
                        break;
                    case ELEMENT:
                        elementExampleLogs.add((SpiderFlowElementExampleLog) elementExampleLog.getExampleLog());
                        break;
                }
            }
            spiderFlowElementExampleService.upsertBatchFlowElementExampleLog(elementExampleLogs);
            exampleLogService.upsetBatchFlowExampleLog(flowExampleLogs);
        } catch (Exception e) {
            log.error("缓存队列批量消费异常：{}", ExceptionMessage.getStackTrace(e));
        }
    }

    public void registerTimer() {
        this.vertx.setPeriodic(2000, id -> {
            vertx.executeBlocking(promise -> {
                consumerByBatch();
            });
        });
    }
}
