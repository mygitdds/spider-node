package cn.spider.framework.controller.data;

import lombok.Builder;
import lombok.Data;

/**
 * @BelongsProject: spider-node
 * @BelongsPackage: cn.spider.framework.controller.data
 * @Author: dengdongsheng
 * @CreateTime: 2023-04-20  00:52
 * @Description: TODO
 * @Version: 1.0
 */

@Builder
@Data
public class RegisterLeaderRequest {
    private String brokerName;

    private String brokerIp;
}
