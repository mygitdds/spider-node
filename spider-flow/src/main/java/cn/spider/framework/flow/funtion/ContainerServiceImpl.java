package cn.spider.framework.flow.funtion;

import cn.spider.framework.common.data.enums.BpmnStatus;
import cn.spider.framework.common.event.EventManager;
import cn.spider.framework.common.event.EventType;
import cn.spider.framework.common.event.data.DestroyBpmnData;
import cn.spider.framework.common.event.data.DestroyClassData;
import cn.spider.framework.common.event.data.LoaderClassData;
import cn.spider.framework.common.event.data.DeployBpmnData;
import cn.spider.framework.common.utils.ExceptionMessage;
import cn.spider.framework.container.sdk.data.DestroyBpmn;
import cn.spider.framework.container.sdk.data.DestroyClass;
import cn.spider.framework.container.sdk.data.LoaderClassRequest;
import cn.spider.framework.container.sdk.data.DeployBpmnRequest;
import cn.spider.framework.container.sdk.interfaces.ContainerService;
import cn.spider.framework.flow.load.loader.ClassLoaderManager;
import cn.spider.framework.flow.resource.factory.StartEventFactory;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.WorkerExecutor;
import io.vertx.core.json.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @BelongsProject: spider-node
 * @BelongsPackage: cn.spider.framework.flow.funtion
 * @Author: dengdongsheng
 * @CreateTime: 2023-03-26  18:07
 * @Description: 功能生命周期管理实习类
 * @Version: 1.0
 */
@Slf4j
@Component
public class ContainerServiceImpl implements ContainerService {

    @Resource
    private StartEventFactory startEventFactory;

    @Resource
    private WorkerExecutor businessExecute;

    @Resource
    private ClassLoaderManager classLoaderManager;

    @Resource
    private EventManager eventManager;

    /**
     * 部署bpmn
     *
     * @param data
     * @return
     */
    @Override
    public Future<Void> deployBpmn(JsonObject data) {
        Promise<Void> promise = Promise.promise();
        businessExecute.executeBlocking(promises -> {
            try {
                DeployBpmnRequest request = data.mapTo(DeployBpmnRequest.class);
                startEventFactory.dynamicsLoaderBpmn(request.getBpmnName());
                DeployBpmnData deployBpmnData = data.mapTo(DeployBpmnData.class);
                deployBpmnData.setStatus(BpmnStatus.ENABLE);
                eventManager.sendMessage(EventType.DEPLOY_BPMN, deployBpmnData);
                promises.complete();
            } catch (Exception e) {
                log.error(ExceptionMessage.getStackTrace(e));
                promises.fail(e);
            }
        }).onSuccess(suss -> {
            promise.complete();
            // 进行同步
        }).onFailure(fail -> {
            promise.fail(fail);
        });

        return promise.future();
    }

    @Override
    public Future<Void> destroyBpmn(JsonObject data) {
        DestroyBpmn request = data.mapTo(DestroyBpmn.class);
        startEventFactory.destroyBpmn(request.getBpmnName());
        DestroyBpmnData destroyBpmnData = DestroyBpmnData.builder()
                .bpmnName(request.getBpmnName())
                .build();
        eventManager.sendMessage(EventType.DEPLOY_BPMN, destroyBpmnData);
        return Future.succeededFuture();
    }

    @Override
    public Future<Void> loaderClass(JsonObject data) {
        Promise<Void> respond = Promise.promise();
        log.info("loaderClass 请求参数 {}",data.toString());
        LoaderClassRequest request = data.mapTo(LoaderClassRequest.class);
        businessExecute.executeBlocking(promise -> {
            // 下载jar文件
            try {
                classLoaderManager.loaderUrlJar(request.getJarName(), request.getClassPath());
                LoaderClassData loaderClassData = data.mapTo(LoaderClassData.class);
                eventManager.sendMessage(EventType.LOADER_JAR, loaderClassData);
                promise.complete();
            } catch (Exception e) {
                log.error(ExceptionMessage.getStackTrace(e));
                promise.fail(e);
            }
        }).onSuccess(suss -> {
            respond.complete();
        }).onFailure(fail -> {
            respond.fail(fail);
        });
        return respond.future();
    }

    @Override
    public Future<Void> destroyClass(JsonObject data) {
        DestroyClass destroyClass = data.mapTo(DestroyClass.class);
        classLoaderManager.unloadJar(destroyClass.getJarName());
        DestroyClassData destroyClassData = DestroyClassData.builder()
                .jarName(destroyClass.getJarName())
                .build();
        eventManager.sendMessage(EventType.DESTROY_JAR, destroyClassData);
        return Future.succeededFuture();
    }


}
