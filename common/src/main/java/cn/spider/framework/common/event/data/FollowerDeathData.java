package cn.spider.framework.common.event.data;

import lombok.Builder;
import lombok.Data;

/**
 * @BelongsProject: spider-node
 * @BelongsPackage: cn.spider.framework.common.event.data
 * @Author: dengdongsheng
 * @CreateTime: 2023-04-20  01:13
 * @Description: 通知节点死亡的实体
 * @Version: 1.0
 */
@Builder
@Data
public class FollowerDeathData extends EventData {
    private String brokerName;
}
