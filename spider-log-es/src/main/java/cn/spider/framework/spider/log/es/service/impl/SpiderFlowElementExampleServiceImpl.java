package cn.spider.framework.spider.log.es.service.impl;

import cn.spider.framework.log.sdk.data.FlowElementExample;
import cn.spider.framework.log.sdk.data.QueryFlowElementExample;
import cn.spider.framework.log.sdk.data.QueryFlowElementExampleResponse;
import cn.spider.framework.spider.log.es.dao.SpiderFlowElementExampleLogDao;
import cn.spider.framework.spider.log.es.domain.SpiderFlowElementExampleLog;
import cn.spider.framework.spider.log.es.service.SpiderFlowElementExampleService;
import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

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

    @Autowired
    private ElasticsearchRestTemplate template;

    @Autowired
    private SpiderFlowElementExampleLogDao spiderFlowElementExampleLogDao;


    @PostConstruct
    public void init() {
        // 创建索引
        template.createIndex(SpiderFlowElementExampleLog.class);
    }

    /**
     * 批量更新
     *
     * @param logs
     */
    @Override
    public void upsertBatchFlowElementExampleLog(List<SpiderFlowElementExampleLog> logs) {
        if(CollectionUtils.isEmpty(logs)){
            return;
        }
        Map<String, List<SpiderFlowElementExampleLog>> flowElementMap = logs.stream().collect(Collectors.groupingBy(SpiderFlowElementExampleLog::getId));
        List<SpiderFlowElementExampleLog> logsNew = Lists.newArrayList();
        for (String key : flowElementMap.keySet()) {
            Optional<SpiderFlowElementExampleLog> spiderFlowElementExampleLog = spiderFlowElementExampleLogDao.findById(key);
            Map<String,Object> spiderFlowMap = Maps.newHashMap();
            if(spiderFlowElementExampleLog.isPresent()){
                SpiderFlowElementExampleLog elementExampleLog = spiderFlowElementExampleLog.get();
                Map<String, Object> ben2Map =
                        JSON.parseObject(JSON.toJSONString(elementExampleLog), Map.class);
                spiderFlowMap.putAll(ben2Map);
            }
            List<SpiderFlowElementExampleLog> logList = flowElementMap.get(key);
            for(SpiderFlowElementExampleLog logNew : logList){
                Map<String, Object> ben2Map =
                        JSON.parseObject(JSON.toJSONString(logNew), Map.class);
                spiderFlowMap.putAll(ben2Map);
            }
            logsNew.add(JSON.parseObject(JSON.toJSONString(spiderFlowMap), SpiderFlowElementExampleLog.class));
        }
        log.info("插入的数据为 {}",JSON.toJSONString(logsNew));
        spiderFlowElementExampleLogDao.saveAll(logsNew);
    }

    @Override
    public QueryFlowElementExampleResponse queryFlowElementExampleLog(QueryFlowElementExample queryFlowElementExample) {
        QueryFlowElementExampleResponse response = new QueryFlowElementExampleResponse();
        Pageable pageable = PageRequest.of(queryFlowElementExample.getPage(), queryFlowElementExample.getSize(), Sort.Direction.DESC, "startTime");
        Page<SpiderFlowElementExampleLog> logPage = spiderFlowElementExampleLogDao.search(buildFlowElementExample(queryFlowElementExample), pageable);
        List<SpiderFlowElementExampleLog> flowElementExampleLogs = logPage.toList();
        List<FlowElementExample> elementExampleList = flowElementExampleLogs.stream().map(item -> {
            FlowElementExample flowElementExample = new FlowElementExample();
            BeanUtils.copyProperties(item, flowElementExample);
            return flowElementExample;
        }).collect(Collectors.toList());
        response.setElementExampleList(elementExampleList);
        response.setTotal(logPage.getTotalElements());
        return response;
    }

    @Override
    public void deleteIndex() {
        template.deleteIndex(SpiderFlowElementExampleLog.class);
    }

    private BoolQueryBuilder buildFlowElementExample(QueryFlowElementExample queryFlowElementExample) {

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
        return defaultQueryBuilder;
    }


}
