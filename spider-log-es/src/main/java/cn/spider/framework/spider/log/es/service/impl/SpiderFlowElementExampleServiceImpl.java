package cn.spider.framework.spider.log.es.service.impl;

import cn.spider.framework.log.sdk.data.QueryFlowElementExample;
import cn.spider.framework.spider.log.es.dao.SpiderFlowElementExampleLogDao;
import cn.spider.framework.spider.log.es.domain.SpiderFlowElementExampleLog;
import cn.spider.framework.spider.log.es.service.SpiderFlowElementExampleService;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.query.*;
import org.springframework.stereotype.Service;

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
@Service
public class SpiderFlowElementExampleServiceImpl implements SpiderFlowElementExampleService {

    @Resource
    private SpiderFlowElementExampleLogDao flowElementExampleLogDao;


    /**
     * 批量更新
     *
     * @param logs
     */
    @Override
    public void upsertBatchFlowElementExampleLog(List<SpiderFlowElementExampleLog> logs) {
        flowElementExampleLogDao.saveAll(logs);
    }

    @Override
    public List<SpiderFlowElementExampleLog> queryFlowElementExampleLog(QueryFlowElementExample queryFlowElementExample) {
        SearchQuery query = buildFlowElementExample(queryFlowElementExample);
        Page<SpiderFlowElementExampleLog> page = flowElementExampleLogDao.search(query);
        return page.getContent();
    }

    private SearchQuery buildFlowElementExample(QueryFlowElementExample queryFlowElementExample) {
        Pageable pageable = new PageRequest(queryFlowElementExample.getPage(), queryFlowElementExample.getSize());

        BoolQueryBuilder defaultQueryBuilder = QueryBuilders.boolQuery();

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

        return new NativeSearchQueryBuilder()
                .withQuery(defaultQueryBuilder)
                .withPageable(pageable)
                .build();
    }


}
