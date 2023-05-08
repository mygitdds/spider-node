package cn.spider.framework.common.event.data;

import lombok.Builder;
import lombok.Data;

/**
 * @BelongsProject: spider-node
 * @BelongsPackage: cn.spider.framework.common.event.data
 * @Author: dengdongsheng
 * @CreateTime: 2023-05-08  14:27
 * @Description: TODO
 * @Version: 1.0
 */
@Builder
@Data
public class DestroyClassData extends EventData  {
    private String jarName;
}
