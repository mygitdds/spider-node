package cn.spider.framework.controller.impl;
import cn.spider.framework.controller.sdk.interfaces.LeaderHeartService;
import io.vertx.core.Future;
import org.springframework.stereotype.Component;

/**
 * @BelongsProject: spider-node
 * @BelongsPackage: cn.spider.framework.controller.impl
 * @Author: dengdongsheng
 * @CreateTime: 2023-04-23  11:36
 * @Description: TODO
 * @Version: 1.0
 */
@Component
public class LeaderHeartServiceImpl implements LeaderHeartService {

    /**
     * 检测leader是否存活
     * @return
     */
    @Override
    public Future<Void> detection() {
        return Future.succeededFuture();
    }
}
