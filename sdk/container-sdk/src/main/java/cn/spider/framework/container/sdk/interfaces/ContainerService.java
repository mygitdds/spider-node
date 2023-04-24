package cn.spider.framework.container.sdk.interfaces;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

/**
 * @program: spider-node
 * @description: ContainerService操作service
 * @author: dds
 * @create: 2023-02-22 22:06
 */
@ProxyGen
@VertxGen
public interface ContainerService {

    String ADDRESS = "CONTAINER_SERVICE";

    static ContainerService createProxy(Vertx vertx, String address) {
        return new ContainerServiceVertxEBProxy(vertx, address);
    }
    // 注册任务信息
    Future<Void> registerFunction(JsonObject data);
    // 销毁
    Future<Void> destroyFunction(JsonObject data);

    // 加载 class部署具体的接口信息
    Future<Void> loaderClass(JsonObject data);


}
