package cn.spider.framework.common.event.data;

import lombok.Builder;
import lombok.Data;

/**
 * @BelongsProject: spider-node
 * @BelongsPackage: cn.spider.framework.common.event.data
 * @Author: dengdongsheng
 * @CreateTime: 2023-04-20  01:15
 * @Description: TODO
 * @Version: 1.0
 */
@Builder
@Data
public class NotifyLeaderCommissionData extends EventData {
    private String brokerName;

    private String brokerIp;
}