package cn.spider.framework.spider.log.es.service;

import cn.spider.framework.log.sdk.data.QueryFlowElementExample;
import cn.spider.framework.spider.log.es.domain.SpiderFlowElementExampleLog;

import java.util.List;

/**
 * @BelongsProject: spider-node
 * @BelongsPackage: cn.spider.framework.spider.log.es.service
 * @Author: dengdongsheng
 * @CreateTime: 2023-04-18  17:10
 * @Description: 流程实例的节点
 * @Version: 1.0
 */
public interface SpiderFlowElementExampleService {

    void upsertBatchFlowElementExampleLog(List<SpiderFlowElementExampleLog> logs);

    List<SpiderFlowElementExampleLog> queryFlowElementExampleLog(QueryFlowElementExample queryFlowElementExample);




}