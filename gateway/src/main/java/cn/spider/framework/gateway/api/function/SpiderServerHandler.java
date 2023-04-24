package cn.spider.framework.gateway.api.function;
import cn.spider.framework.common.utils.ExceptionMessage;
import cn.spider.framework.container.sdk.interfaces.BusinessService;
import cn.spider.framework.container.sdk.interfaces.ContainerService;
import cn.spider.framework.container.sdk.interfaces.FlowService;
import cn.spider.framework.db.list.RedisList;
import cn.spider.framework.gateway.common.ResponseData;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @BelongsProject: spider-node
 * @BelongsPackage: cn.spider.framework.gateway.api.function
 * @Author: dengdongsheng
 * @CreateTime: 2023-03-22  13:33
 * @Description: spider-跟ui交互接口
 * @Version: 1.0
 */
@Slf4j
@Component
public class SpiderServerHandler {

    private Router router;

    @Autowired
    private ContainerService containerService;

    @Autowired
    private FlowService flowService;

    @Resource
    private RedisTemplate redisTemplate;

    @Resource
    private BusinessService businessService;

    private RedisList jarRedisList;

    public void init(Router router) {
        this.router = router;
        this.jarRedisList = new RedisList(redisTemplate,"loaderJar");
        deployBpmnFunction();
        unloadFunction();
        startFlow();
        deployClass();
        registerFunction();
    }

    /**
     * 部署功能
     */
    private void deployBpmnFunction() {
        router.post("/deploy/function")
                .handler(ctx -> {
                    HttpServerResponse response = ctx.response();
                    response.putHeader("content-type", "application/json");
                    JsonObject param = ctx.getBodyAsJson();
                    Future<Void> registerFuture = containerService.registerFunction(param);
                    registerFuture.onSuccess(suss -> {
                        response.end(ResponseData.suss());
                    }).onFailure(fail -> {
                        response.end(ResponseData.fail(fail));
                    });
                });
    }

    /**
     * 部署功能
     */
    private void unloadFunction() {
        router.post("/unload/function")
                .handler(ctx -> {
                    HttpServerResponse response = ctx.response();
                    response.putHeader("content-type", "application/json");
                    JsonObject param = ctx.getBodyAsJson();
                    Future<Void> destroyrFuture = containerService.destroyFunction(param);
                    destroyrFuture.onSuccess(suss -> {
                        response.end(ResponseData.suss());
                    }).onFailure(fail -> {
                        response.end(ResponseData.fail(fail));
                    });
                });
    }

    private void startFlow() {
        router.post("/start/function")
                .handler(ctx -> {
                    HttpServerResponse response = ctx.response();
                    response.putHeader("content-type", "application/json");
                    JsonObject param = ctx.getBodyAsJson();
                    Future<JsonObject> flowFuture = flowService.startFlow(param);
                    flowFuture.onSuccess(suss -> {
                        JsonObject result = suss;
                        response.end(ResponseData.suss(result));
                    }).onFailure(fail -> {
                        response.end(ResponseData.fail(fail));
                    });
                });
    }

    private void deployClass() {
        router.post("/deploy/class")
                .handler(ctx -> {
                    HttpServerResponse response = ctx.response();
                    response.putHeader("content-type", "application/json");
                    JsonObject param = ctx.getBodyAsJson();
                    Future<Void> registerFuture = containerService.loaderClass(param);
                    registerFuture.onSuccess(suss -> {
                        response.end(ResponseData.suss());
                        this.jarRedisList.addQueue(param.toString());
                    }).onFailure(fail -> {
                        response.end(ResponseData.fail(fail));
                    });
                });
    }

    /**
     * 新增具体的业务功能
     */
    private void registerFunction(){
        router.post("/register/function")
                .handler(ctx -> {
                    HttpServerResponse response = ctx.response();
                    response.putHeader("content-type", "application/json");
                    JsonObject param = ctx.getBodyAsJson();
                    Future<JsonObject> registerFuture = businessService.registerFunction(param);
                    registerFuture.onSuccess(suss -> {
                        response.end(ResponseData.suss(suss));
                    }).onFailure(fail -> {
                        log.error("/register/function注册失败 {}", ExceptionMessage.getStackTrace(fail));
                        response.end(ResponseData.fail(fail));
                    });
                });
    }


}
