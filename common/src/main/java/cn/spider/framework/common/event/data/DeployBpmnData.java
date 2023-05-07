package cn.spider.framework.common.event.data;

import lombok.Builder;
import lombok.Data;

/**
 * @BelongsProject: spider-node
 * @BelongsPackage: cn.spider.framework.container.sdk.data
 * @Author: dengdongsheng
 * @CreateTime: 2023-03-26  18:16
 * @Description: TODO
 * @Version: 1.0
 */
@Builder
@Data
public class DeployBpmnData extends EventData {
    private String functionName;

    private String bpmnName;
}
