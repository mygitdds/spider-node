package cn.spider.framework.container.sdk.interfaces;

import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

/**
 * @BelongsProject: spider-node
 * @BelongsPackage: cn.spider.framework.container.sdk.interfaces
 * @Author: dengdongsheng
 * @CreateTime: 2023-04-12  14:03
 * @Description: TODO
 * @Version: 1.0
 */
@ProxyGen
@VertxGen
public interface BusinessService {
    String ADDRESS = "BUSINESS_SERVICE";

    static BusinessService createProxy(Vertx vertx, String address) {
        return new BusinessServiceVertxEBProxy(vertx, address);
    }

    // 注册功能
    Future<JsonObject> registerFunction(JsonObject data);
    // 设置开关
    Future<Void> configureDerail(JsonObject data);
    // 配置权重
    Future<Void> configureWeight(JsonObject data);

}