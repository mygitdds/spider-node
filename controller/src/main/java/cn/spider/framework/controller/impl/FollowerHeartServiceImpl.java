package cn.spider.framework.controller.impl;

import cn.spider.framework.common.utils.SpringUtil;
import cn.spider.framework.controller.follower.FollowerManager;
import cn.spider.framework.controller.sdk.interfaces.FollowerHeartService;
import io.vertx.core.Future;
import io.vertx.core.Promise;

/**
 * @BelongsProject: spider-node
 * @BelongsPackage: cn.spider.framework.controller.impl
 * @Author: dengdongsheng
 * @CreateTime: 2023-04-23  11:30
 * @Description: TODO
 * @Version: 1.0
 */
public class FollowerHeartServiceImpl implements FollowerHeartService {

    /**
     * 直接返回成功用于 检测是否存货
     *
     * @return
     */
    @Override
    public Future<Void> detection() {
        return Future.succeededFuture();
    }

    /**
     * 重新跟leader建立链接
     * @return
     */
    @Override
    public Future<Void> reconnectLeader() {
        Promise<Void> promise = Promise.promise();
        FollowerManager followerManager = SpringUtil.getBean(FollowerManager.class);
        Future<Void> connectFuture = followerManager.connect();
        connectFuture.onSuccess(suss->{
            promise.complete();
        }).onFailure(fail->{
            promise.fail(fail);
        });
        return promise.future();
    }
}
