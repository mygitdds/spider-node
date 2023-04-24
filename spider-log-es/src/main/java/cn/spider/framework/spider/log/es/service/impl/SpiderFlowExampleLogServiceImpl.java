package cn.spider.framework.spider.log.es.service.impl;
import cn.spider.framework.log.sdk.data.FlowExample;
import cn.spider.framework.log.sdk.data.QueryFlowExample;
import cn.spider.framework.log.sdk.data.QueryFlowExampleResponse;
import cn.spider.framework.spider.log.es.dao.SpiderFlowExampleLogDao;
import cn.spider.framework.spider.log.es.domain.SpiderFlowExampleLog;
import cn.spider.framework.spider.log.es.service.SpiderFlowExampleLogService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.query.*;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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
    private SpiderFlowExampleLogDao exampleLogDao;

    /**
     * 批量新增
     *
     * @param logs
     */
    public void upsetBatchFlowExampleLog(List<SpiderFlowExampleLog> logs) {

        if (CollectionUtils.isEmpty(logs)) {
            return;
        }
        exampleLogDao.saveAll(logs);
    }


    public QueryFlowExampleResponse queryFlowExampleLog(QueryFlowExample queryFlowExample){
        SearchQuery query = buildFlowExample(queryFlowExample);
        Page<SpiderFlowExampleLog> page = exampleLogDao.search(query);
        QueryFlowExampleResponse exampleResponse = new QueryFlowExampleResponse();
        if(page.isEmpty()){
            return exampleResponse;
        }
        List<FlowExample> flowExampleList = page.stream().map(item->{
            FlowExample flowExample = new FlowExample();
            BeanUtils.copyProperties(item,flowExample);
            return flowExample;
        }).collect(Collectors.toList());

        exampleResponse.setFlowExampleList(flowExampleList);
        exampleResponse.setTotal(page.getTotalPages());
        return exampleResponse;
    }

    private SearchQuery buildFlowExample(QueryFlowExample queryFlowExample) {

        Pageable pageable = new PageRequest(queryFlowExample.getPage(), queryFlowExample.getSize());

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


        return new NativeSearchQueryBuilder()
                .withQuery(defaultQueryBuilder)
                .withPageable(pageable)
                .build();
    }
}
