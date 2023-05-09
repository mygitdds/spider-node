package cn.spider.framework.gateway.api.function;

import cn.spider.framework.common.config.Constant;
import cn.spider.framework.common.data.enums.BpmnStatus;
import cn.spider.framework.common.data.enums.JarStatus;
import cn.spider.framework.common.utils.ExceptionMessage;
import cn.spider.framework.container.sdk.data.DeployBpmnRequest;
import cn.spider.framework.container.sdk.data.DestroyBpmn;
import cn.spider.framework.container.sdk.data.DestroyClass;
import cn.spider.framework.container.sdk.data.LoaderClassRequest;
import cn.spider.framework.container.sdk.interfaces.BusinessService;
import cn.spider.framework.container.sdk.interfaces.ContainerService;
import cn.spider.framework.container.sdk.interfaces.FlowService;
import cn.spider.framework.db.list.RedisList;
import cn.spider.framework.db.map.RedisMap;
import cn.spider.framework.gateway.common.ResponseData;
import cn.spider.framework.log.sdk.interfaces.LogInterface;
import com.alibaba.fastjson.JSON;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    @Resource
    private LogInterface logInterface;

    private RedisMap jarRedisMap;

    private RedisMap bpmnRedisMap;

    public void init(Router router) {
        this.router = router;
        deployBpmnFunction();
        unloadFunction();
        startFlow();
        deployClass();
        registerFunction();
        queryElementInfo();
        queryFlowExampleInfo();
        destroyBpmn();
        jarRedisMap = new RedisMap(redisTemplate, Constant.JAR_KEY);
        bpmnRedisMap = new RedisMap(redisTemplate, Constant.BPMN_KEY);
        cleanRedisList();
        selectBpmn();
        selectJar();
        selectFunction();
        deleteBpmn();
        functionStateChange();
        deleteFunction();
        deleteAllFunction();
    }

    public void selectBpmn() {
        router.post("/select/bpmn")
                .handler(ctx -> {
                    HttpServerResponse response = ctx.response();
                    response.putHeader("content-type", "application/json");
                    try {
                        Map<String, String> bpmnMap = bpmnRedisMap.getHash();
                        List<JsonObject> results = bpmnMap.values().stream().map(item -> new JsonObject(item)).collect(Collectors.toList());
                        response.end(ResponseData.sussObject(results));
                    } catch (Exception e) {
                        response.end(ResponseData.fail(e));
                    }
                });
    }

    public void selectJar() {
        router.post("/select/jar")
                .handler(ctx -> {
                    HttpServerResponse response = ctx.response();
                    response.putHeader("content-type", "application/json");
                    try {
                        Map<String, String> jarMap = jarRedisMap.getHash();
                        List<JsonObject> results = jarMap.values().stream().map(item -> new JsonObject(item)).collect(Collectors.toList());
                        response.end(ResponseData.sussObject(results));
                    } catch (Exception e) {
                        response.end(ResponseData.fail(e));
                    }
                });
    }


    public void cleanRedisList() {
        router.post("/clean/redis-list")
                .handler(ctx -> {
                    HttpServerResponse response = ctx.response();
                    response.putHeader("content-type", "application/json");
                    try {
                        Map<String, String> jarMap = jarRedisMap.getHash();
                        jarMap.keySet().forEach(item -> {
                            jarRedisMap.remove(item);
                        });

                        Map<String, String> bpmnMap = bpmnRedisMap.getHash();
                        bpmnMap.keySet().forEach(item -> {
                            bpmnRedisMap.remove(item);
                        });
                        response.end(ResponseData.suss());
                    } catch (Exception e) {
                        response.end(ResponseData.fail(e));
                    }
                });
    }

    /**
     * 部署bpmn
     */
    private void deployBpmnFunction() {
        router.post("/deploy/bpmn")
                .handler(ctx -> {
                    HttpServerResponse response = ctx.response();
                    response.putHeader("content-type", "application/json");
                    JsonObject param = ctx.getBodyAsJson();
                    Future<Void> registerFuture = containerService.deployBpmn(param);
                    registerFuture.onSuccess(suss -> {
                        DeployBpmnRequest deployBpmnRequest = param.mapTo(DeployBpmnRequest.class);
                        deployBpmnRequest.setStatus(BpmnStatus.ENABLE);
                        bpmnRedisMap.put(deployBpmnRequest.getBpmnName(), JSON.toJSONString(deployBpmnRequest));
                        response.end(ResponseData.suss());
                    }).onFailure(fail -> {
                        response.end(ResponseData.fail(fail));
                    });
                });
    }

    private void deleteBpmn() {
        router.post("/delete/bpmn")
                .handler(ctx -> {
                    HttpServerResponse response = ctx.response();
                    response.putHeader("content-type", "application/json");
                    JsonObject param = ctx.getBodyAsJson();
                    bpmnRedisMap.remove(param.getString("bpmnKey"));
                    response.end(ResponseData.suss());
                });
    }


    private void destroyBpmn() {
        router.post("/destroy/bpmn")
                .handler(ctx -> {
                    HttpServerResponse response = ctx.response();
                    response.putHeader("content-type", "application/json");
                    JsonObject param = ctx.getBodyAsJson();
                    Future<Void> registerFuture = containerService.destroyBpmn(param);
                    registerFuture.onSuccess(suss -> {
                        try {
                            DestroyBpmn destroyBpmn = param.mapTo(DestroyBpmn.class);
                            String bpmnData = (String) bpmnRedisMap.get(destroyBpmn.getBpmnName());
                            DeployBpmnRequest bpmnRequest = JSON.parseObject(bpmnData, DeployBpmnRequest.class);
                            bpmnRequest.setStatus(BpmnStatus.STOP);
                            bpmnRedisMap.put(bpmnRequest.getBpmnName(), JSON.toJSONString(bpmnRequest));
                        } catch (Exception e) {
                            response.end(ResponseData.fail(e));
                            return;
                        }
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
                        LoaderClassRequest request = param.mapTo(LoaderClassRequest.class);
                        jarRedisMap.put(request.getJarName(), JSON.toJSONString(request));
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
        router.post("/unload/class")
                .handler(ctx -> {
                    HttpServerResponse response = ctx.response();
                    response.putHeader("content-type", "application/json");
                    JsonObject param = ctx.getBodyAsJson();
                    Future<Void> destroyrFuture = containerService.destroyClass(param);
                    destroyrFuture.onSuccess(suss -> {
                        DestroyClass destroyClass = param.mapTo(DestroyClass.class);
                        String jar = (String) jarRedisMap.get(destroyClass.getJarName());
                        LoaderClassRequest request = JSON.parseObject(jar, LoaderClassRequest.class);
                        jarRedisMap.put(request.getJarName(), JSON.toJSONString(request));
                        response.end(ResponseData.suss());
                    }).onFailure(fail -> {
                        response.end(ResponseData.fail(fail));
                    });
                });
    }

    /**
     * 新增具体的业务功能
     */
    private void registerFunction() {
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

    private void deleteFunction() {
        router.post("/delete/function")
                .handler(ctx -> {
                    HttpServerResponse response = ctx.response();
                    response.putHeader("content-type", "application/json");
                    JsonObject param = ctx.getBodyAsJson();
                    Future<Void> deleteFuture = businessService.deleteFunction(param);
                    deleteFuture.onSuccess(suss -> {
                        response.end(ResponseData.suss());
                    }).onFailure(fail -> {
                        log.error("/delete/function删除失败{}", ExceptionMessage.getStackTrace(fail));
                        response.end(ResponseData.fail(fail));
                    });
                });
    }

    private void functionStateChange() {
        router.post("/state/function/change")
                .handler(ctx -> {
                    HttpServerResponse response = ctx.response();
                    response.putHeader("content-type", "application/json");
                    JsonObject param = ctx.getBodyAsJson();
                    Future<Void> stateChangeFuture = businessService.stateChange(param);
                    stateChangeFuture.onSuccess(suss -> {
                        response.end(ResponseData.suss());
                    }).onFailure(fail -> {
                        log.error("/state/function/change更改状态失败 {}", ExceptionMessage.getStackTrace(fail));
                        response.end(ResponseData.fail(fail));
                    });
                });
    }



    private void selectFunction() {
        router.post("/query/function")
                .handler(ctx -> {
                    HttpServerResponse response = ctx.response();
                    response.putHeader("content-type", "application/json");
                    Future<JsonObject> elementResponse = businessService.selectFunction(new JsonObject());
                    elementResponse.onSuccess(suss -> {
                        response.end(ResponseData.sussJson(suss));
                    }).onFailure(fail -> {
                        log.error("/query/function查询失败 {}", ExceptionMessage.getStackTrace(fail));
                        response.end(ResponseData.fail(fail));
                    });
                });
    }

    private void queryElementInfo() {
        router.post("/query/elementInfo")
                .handler(ctx -> {
                    HttpServerResponse response = ctx.response();
                    response.putHeader("content-type", "application/json");
                    JsonObject param = ctx.getBodyAsJson();
                    Future<JsonObject> elementResponse = logInterface.queryExampleExample(param);
                    elementResponse.onSuccess(suss -> {
                        response.end(ResponseData.suss(suss));
                    }).onFailure(fail -> {
                        log.error("/query/elementInfo查询失败 {}", ExceptionMessage.getStackTrace(fail));
                        response.end(ResponseData.fail(fail));
                    });
                });
    }

    private void queryFlowExampleInfo() {
        router.post("/query/flowExampleInfo")
                .handler(ctx -> {
                    HttpServerResponse response = ctx.response();
                    response.putHeader("content-type", "application/json");
                    JsonObject param = ctx.getBodyAsJson();
                    Future<JsonObject> elementResponse = logInterface.queryFlowExample(param);
                    elementResponse.onSuccess(suss -> {
                        response.end(ResponseData.suss(suss));
                    }).onFailure(fail -> {
                        log.error("/query/flowExampleInfo查询失败 {}", ExceptionMessage.getStackTrace(fail));
                        response.end(ResponseData.fail(fail));
                    });
                });
    }

    private void deleteAllFunction() {
        router.post("/delete/all/function")
                .handler(ctx -> {
                    HttpServerResponse response = ctx.response();
                    response.putHeader("content-type", "application/json");
                    Future<Void> deleteFuture = businessService.deleteAll();
                    deleteFuture.onSuccess(suss -> {
                        response.end(ResponseData.suss());
                    }).onFailure(fail -> {
                        log.error("/delete/all/function {}", ExceptionMessage.getStackTrace(fail));
                        response.end(ResponseData.fail(fail));
                    });
                });
    }


}
