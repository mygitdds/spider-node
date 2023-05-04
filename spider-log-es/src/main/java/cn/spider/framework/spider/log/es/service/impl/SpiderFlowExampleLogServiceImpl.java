package cn.spider.framework.spider.log.es.service.impl;
import cn.spider.framework.log.sdk.data.FlowExample;
import cn.spider.framework.log.sdk.data.QueryFlowExample;
import cn.spider.framework.log.sdk.data.QueryFlowExampleResponse;
import cn.spider.framework.spider.log.es.client.CustomEsClient;
import cn.spider.framework.spider.log.es.client.EsIndexTypeId;
import cn.spider.framework.spider.log.es.client.PageEsData;
import cn.spider.framework.spider.log.es.config.Constant;
import cn.spider.framework.spider.log.es.service.SpiderFlowExampleLogService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

/**
 * @BelongsProject: spider-node
 * @BelongsPackage: cn.spider.framework.spider.log.es.service.impl
 * @Author: dengdongsheng
 * @CreateTime: 2023-04-18  13:10
 * @Description: TODO
 * @Version: 1.0
 */
@Service
public class SpiderFlowExampleLogServiceImpl implements SpiderFlowExampleLogService {

    @Resource
    private CustomEsClient client;


    /**
     * 批量新增
     *
     * @param logs
     */
    public void upsetBatchFlowExampleLog(List<EsIndexTypeId> logs) {

        if (CollectionUtils.isEmpty(logs)) {
            return;
        }
        client.upsertAll(logs);
    }


    public QueryFlowExampleResponse queryFlowExampleLog(QueryFlowExample queryFlowExample){
        QueryFlowExampleResponse response = new QueryFlowExampleResponse();
        PageEsData<FlowExample> page = client.searchPage(buildFlowExample(queryFlowExample),
                Constant.SPIDER_FLOW_ELEMENT_EXAMPLE_LOG_INDEX,Constant.SPIDER_FLOW_ELEMENT_EXAMPLE_LOG_TYPE,FlowExample.class);

        response.setTotal(page.getTotalRows());
        response.setFlowExampleList(page.getData());
        return response;
    }

    private SearchSourceBuilder buildFlowExample(QueryFlowExample queryFlowExample) {

        SearchSourceBuilder searchSourceBuilder = SearchSourceBuilder.searchSource();

        BoolQueryBuilder defaultQueryBuilder = QueryBuilders.boolQuery();
        if (StringUtils.isNotEmpty(queryFlowExample.getBusinessParam())) {

            defaultQueryBuilder.should(QueryBuilders.queryStringQuery(queryFlowExample.getBusinessParam()).field("requestParam"));
        }

        if (StringUtils.isNotEmpty(queryFlowExample.getRequestId())) {
            defaultQueryBuilder.should(QueryBuilders.termQuery("requestId", queryFlowExample.getRequestId()));
        }

        if (StringUtils.isNotEmpty(queryFlowExample.getFunctionName())) {
            defaultQueryBuilder.should(QueryBuilders.termQuery("functionName", queryFlowExample.getFunctionName()));
        }

        if (StringUtils.isNotEmpty(queryFlowExample.getFunctionId())) {
            defaultQueryBuilder.should(QueryBuilders.termQuery("functionId", queryFlowExample.getFunctionId()));
        }
        // 出发时间
        if (Objects.nonNull(queryFlowExample.getStartTime())) {
            defaultQueryBuilder.should(QueryBuilders.rangeQuery("startTime").gt(queryFlowExample.getStartTime()));
        }

        if (Objects.nonNull(queryFlowExample.getEndTime())) {
            defaultQueryBuilder.should(QueryBuilders.rangeQuery("endTime").lte(queryFlowExample.getEndTime()));
        }
        // 耗时
        if (Objects.nonNull(queryFlowExample.getGtTakeTime())) {
            defaultQueryBuilder.should(QueryBuilders.rangeQuery("takeTime").gt(queryFlowExample.getGtTakeTime()));
        }

        if (Objects.nonNull(queryFlowExample.getLtTakeTime())) {
            defaultQueryBuilder.should(QueryBuilders.rangeQuery("takeTime").lt(queryFlowExample.getGtTakeTime()));
        }

        searchSourceBuilder.query(defaultQueryBuilder);
        searchSourceBuilder.size(queryFlowExample.getSize());
        searchSourceBuilder.from(queryFlowExample.getPage());
        return searchSourceBuilder;
    }
}
