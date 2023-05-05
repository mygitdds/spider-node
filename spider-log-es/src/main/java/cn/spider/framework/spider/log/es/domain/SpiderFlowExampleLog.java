package cn.spider.framework.spider.log.es.domain;
import cn.spider.framework.spider.log.es.client.EsIndexTypeId;
import cn.spider.framework.spider.log.es.config.Constant;
import com.fasterxml.jackson.annotation.JsonFormat;
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
 * @CreateTime: 2023-04-17  22:31
 * @Description: spider-flow-实例的日志实体
 * @Version: 1.0
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class SpiderFlowExampleLog extends SpiderLog implements Serializable, EsIndexTypeId {

    /**
     * 请求的requestId-当作id存储
     */
    private String id;

    /**
     * 请求参数
     */
    private String requestParam;

    private String returnParam;

    /**
     * 对应的-执行的broker
     */
    private String brokerName;

    /**
     * 执行状态
     */
    private String status;

    /**
     * 异常信息
     */
    private String exception;

    /**
     * 事务状态
     */
    private String transactionStatus;

    /**
     * 功能名称
     */
    private String functionName;

    /**
     * 功能id
     */
    private String functionId;

    /**
     * 开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    private LocalDateTime endTime;


    @Override
    public String index() {
        return Constant.SPIDER_FLOW_EXAMPLE_LOG_INDEX;
    }

    @Override
    public String type() {
        return Constant.SPIDER_FLOW_EXAMPLE_LOG_TYPE;
    }

    @Override
    public Object id() {
        return id;
    }
}
