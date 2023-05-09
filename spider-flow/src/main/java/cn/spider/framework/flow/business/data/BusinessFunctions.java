package cn.spider.framework.flow.business.data;

import cn.spider.framework.flow.business.enums.FunctionStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @BelongsProject: spider-node
 * @BelongsPackage: cn.spider.framework.flow.business
 * @Author: dengdongsheng
 * @CreateTime: 2023-04-11  16:34
 * @Description: TODO
 * @Version: 1.0
 */
@Data
public class BusinessFunctions {
    /**
     * 功能名称
     */
    private String name;

    /**
     *
     */
    private String id;

    /**
     * 功能版本
     */
    private String version;

    /**
     * 启动流程的startId
     */
    private String startId;

    /**
     * 业务功能状态
     */
    private FunctionStatus status;

    /**
     * 描述
     */
    private String desc;

    /**
     * 创建时间
     */
    private String createTime;

    /**
     * bpmn的文件名称
     */
    private String bpmnName;


}
