package cn.spider.framework.spider.log.es.service.impl;
import cn.spider.framework.log.sdk.data.FlowElementExample;
import cn.spider.framework.log.sdk.data.QueryFlowElementExample;
import cn.spider.framework.log.sdk.data.QueryFlowElementExampleResponse;
import cn.spider.framework.spider.log.es.client.CustomEsClient;
import cn.spider.framework.spider.log.es.client.EsIndexTypeId;
import cn.spider.framework.spider.log.es.client.PageEsData;
import cn.spider.framework.spider.log.es.config.Constant;
import cn.spider.framework.spider.log.es.domain.SpiderFlowElementExampleLog;
import cn.spider.framework.spider.log.es.service.SpiderFlowElementExampleService;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

/**
 * @BelongsProject: spider-node
 * @BelongsPackage: cn.spider.framework.spider.log.es.service.impl
 * @Author: dengdongsheng
 * @CreateTime: 2023-04-18  18:14
 * @Description: 流程节点的操作实现类
 * @Version: 1.0
 */
@Slf4j
@Service
public class SpiderFlowElementExampleServiceImpl implements SpiderFlowElementExampleService {

    @Resource
    private CustomEsClient client;

    @PostConstruct
    public void init(){
        SpiderFlowElementExampleLog elementExampleLog = new SpiderFlowElementExampleLog();
        client.createIndex(elementExampleLog);
    }

    /**
     * 批量更新
     *
     * @param logs
     */
    @Override
    public void upsertBatchFlowElementExampleLog(List<EsIndexTypeId> logs) {
        client.upsertAll(logs);
    }

    @Override
    public QueryFlowElementExampleResponse queryFlowElementExampleLog(QueryFlowElementExample queryFlowElementExample) {
        QueryFlowElementExampleResponse response = new QueryFlowElementExampleResponse();
        PageEsData<FlowElementExample> page = client.searchPage(buildFlowElementExample(queryFlowElementExample),
                Constant.SPIDER_FLOW_ELEMENT_EXAMPLE_LOG_INDEX,Constant.SPIDER_FLOW_ELEMENT_EXAMPLE_LOG_TYPE,FlowElementExample.class);
        response.setElementExampleList(page.getData());
        response.setTotal(page.getTotalRows());
        return response;
    }

    private SearchSourceBuilder buildFlowElementExample(QueryFlowElementExample queryFlowElementExample) {

        BoolQueryBuilder defaultQueryBuilder = QueryBuilders.boolQuery();

        SearchSourceBuilder searchSourceBuilder = SearchSourceBuilder.searchSource();

        if (StringUtils.isNotEmpty(queryFlowElementExample.getRequestId())) {
            defaultQueryBuilder.should(QueryBuilders.termQuery("requestId", queryFlowElementExample.getRequestId()));
        }

        if (StringUtils.isNotEmpty(queryFlowElementExample.getRequestParam())) {
            defaultQueryBuilder.should(QueryBuilders.queryStringQuery(queryFlowElementExample.getRequestParam()).field("requestParam"));
        }

        if (StringUtils.isNotEmpty(queryFlowElementExample.getReturnParam())) {
            defaultQueryBuilder.should(QueryBuilders.queryStringQuery(queryFlowElementExample.getReturnParam()).field("returnParam"));
        }

        if (StringUtils.isNotEmpty(queryFlowElementExample.getFunctionName())) {
            defaultQueryBuilder.should(QueryBuilders.termQuery("functionName", queryFlowElementExample.getFunctionName()));
        }

        if (StringUtils.isNotEmpty(queryFlowElementExample.getFunctionId())) {
            defaultQueryBuilder.should(QueryBuilders.termQuery("functionId", queryFlowElementExample.getFunctionId()));
        }

        if (Objects.nonNull(queryFlowElementExample.getStartTime())) {
            defaultQueryBuilder.should(QueryBuilders.rangeQuery("startTime").gt(queryFlowElementExample.getStartTime()));
        }

        if (Objects.nonNull(queryFlowElementExample.getEndTime())) {
            defaultQueryBuilder.should(QueryBuilders.rangeQuery("startTime").lte(queryFlowElementExample.getEndTime()));
        }


        // 耗时
        if (Objects.nonNull(queryFlowElementExample.getGtTakeTime())) {
            defaultQueryBuilder.should(QueryBuilders.rangeQuery("takeTime").gt(queryFlowElementExample.getGtTakeTime()));
        }

        if (Objects.nonNull(queryFlowElementExample.getLtTakeTime())) {
            defaultQueryBuilder.should(QueryBuilders.rangeQuery("takeTime").lt(queryFlowElementExample.getGtTakeTime()));
        }
        searchSourceBuilder.query(defaultQueryBuilder);
        searchSourceBuilder.size(queryFlowElementExample.getSize());
        searchSourceBuilder.from(queryFlowElementExample.getPage());
        return searchSourceBuilder;

    }


}
