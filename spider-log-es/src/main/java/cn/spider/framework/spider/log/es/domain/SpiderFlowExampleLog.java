package cn.spider.framework.spider.log.es.domain;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @BelongsProject: spider-node
 * @BelongsPackage: cn.spider.framework.spider.log.es.domain
 * @Author: dengdongsheng
 * @CreateTime: 2023-04-17  22:31
 * @Description: spider-flow-实例的日志实体
 * @Version: 1.0
 */
@Builder
@Document(indexName = "spider-flow-example")
@Data
public class SpiderFlowExampleLog extends SpiderLog implements Serializable {

    /**
     * 请求的requestId-当作id存储
     */
    @Field(type = FieldType.Keyword,index = false,store = true)
    private String id;

    /**
     * 请求参数
     */
    @Field(type = FieldType.Keyword,store = true)
    private String requestParam;

    @Field(type = FieldType.Keyword,store = true)
    private String returnParam;

    /**
     * 对应的-执行的broker
     */
    @Field(type = FieldType.Keyword,store = true)
    private String brokerName;

    /**
     * 执行状态
     */
    @Field(type = FieldType.Keyword,store = true)
    private String status;

    /**
     * 异常信息
     */
    @Field(type = FieldType.Keyword,store = true)
    private String exception;

    /**
     * 事务状态
     */
    @Field(type = FieldType.Keyword,store = true)
    private String transactionStatus;

    @Field(type = FieldType.Keyword,store = true)
    private String functionName;

    @Field(type = FieldType.Keyword,store = true)
    private String functionId;

    /**
     * 开始时间
     */
    @Field(type = FieldType.Date,store = true)
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    @Field(type = FieldType.Date,store = true)
    private LocalDateTime endTime;


}
