package cn.spider.framework.spider.log.es.domain;

import cn.spider.framework.spider.log.es.client.EsIndexTypeId;
import cn.spider.framework.spider.log.es.config.Constant;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
@NoArgsConstructor
@AllArgsConstructor
@Data
public class SpiderFlowElementExampleLog extends SpiderLog implements Serializable, EsIndexTypeId {
    /**
     * 请求id
     */
    private String id;

    private String requestId;

    /**
     * 节点名称
     */
    private String flowElementName;

    /**
     * 节点id
     */
    private String flowElementId;

    /**
     * 功能id
     */
    private String functionId;

    /**
     * 节点执行参数
     */
    private String requestParam;

    /**
     * 功能名称
     */
    private String functionName;

    /**
     * 该节点返回参数
     */
    private String returnParam;

    /**
     * 异常
     */
    private String exception;

    /**
     * 执行状态
     */
    private String status;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 完成时间
     */
    private LocalDateTime finalEndTime;

    /**
     * 事务组
     */
    private String transactionGroupId;

    /**
     * 单个事务id
     */
    private String branchId;

    /**
     * 事务id
     */
    private String transactionStatus;

    /**
     * 事务操作
     */
    private String transactionOperate;

    @Override
    public String index() {
        return Constant.SPIDER_FLOW_ELEMENT_EXAMPLE_LOG_INDEX;
    }

    @Override
    public String type() {
        return Constant.SPIDER_FLOW_ELEMENT_EXAMPLE_LOG_TYPE;
    }

    @Override
    public Object id() {
        return id;
    }
}
