package cn.spider.framework.spider.log.es.domain;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @BelongsProject: spider-node
 * @BelongsPackage: cn.spider.framework.spider.log.es.domain
 * @Author: dengdongsheng
 * @CreateTime: 2023-04-18  17:33
 * @Description: 流程节点日志-实体
 * @Version: 1.0
 */
@Builder
@Document(indexName = "spider-flow-element-example")
@Data
public class SpiderFlowElementExampleLog extends SpiderLog implements Serializable {
    /**
     * 请求id
     */
    @Id
    @Field(type = FieldType.Keyword, store = true)
    private String id;

    @Field(type = FieldType.Keyword, store = true)
    private String requestId;

    /**
     * 节点名称
     */
    @Field(type = FieldType.Keyword, store = true)
    private String flowElementName;

    /**
     * 节点id
     */
    @Field(type = FieldType.Keyword, store = true)
    private String flowElementId;

    /**
     * 功能id
     */
    @Field(type = FieldType.Keyword, store = true)
    private String functionId;

    /**
     * 节点执行参数
     */
    @Field(type = FieldType.Keyword, store = true)
    private String requestParam;

    /**
     * 功能名称
     */
    @Field(type = FieldType.Keyword, store = true)
    private String functionName;

    /**
     * 该节点返回参数
     */
    @Field(type = FieldType.Keyword, store = true)
    private String returnParam;

    /**
     * 异常
     */
    @Field(type = FieldType.Keyword, index = false, store = true)
    private String exception;

    /**
     * 执行状态
     */
    @Field(type = FieldType.Keyword, store = true)
    private String status;

    /**
     * 开始时间
     */
    @Field(type = FieldType.Date, store = true)
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    @Field(type = FieldType.Date, store = true)
    private LocalDateTime endTime;

    @Field(type = FieldType.Date, store = true)
    private LocalDateTime finalEndTime;

    @Field(type = FieldType.Keyword, store = true)
    private String transactionGroupId;

    /**
     * 单个事务id
     */
    @Field(type = FieldType.Keyword, store = true)
    private String branchId;

    /**
     * 事务id
     */
    @Field(type = FieldType.Keyword, store = true)
    private String transactionStatus;

    /**
     * 事务操作
     */
    @Field(type = FieldType.Keyword, store = true)
    private String transactionOperate;

}
