package cn.spider.framework.flow.funtion;

import cn.spider.framework.common.event.EventManager;
import cn.spider.framework.common.event.EventType;
import cn.spider.framework.common.event.data.LoaderClassData;
import cn.spider.framework.common.event.data.DeployBpmnData;
import cn.spider.framework.common.utils.ExceptionMessage;
import cn.spider.framework.container.sdk.data.LoaderClassRequest;
import cn.spider.framework.container.sdk.data.RegisterFunctionRequest;
import cn.spider.framework.container.sdk.interfaces.ContainerService;
import cn.spider.framework.flow.load.loader.ClassLoaderManager;
import cn.spider.framework.flow.resource.factory.StartEventFactory;
import cn.spider.framework.flow.sync.Publish;
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

    @Resource
    private Publish publish;

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
                RegisterFunctionRequest request = data.mapTo(RegisterFunctionRequest.class);
                startEventFactory.dynamicsLoaderBpmn(request.getBpmnName());
                DeployBpmnData registerFunctionData = data.mapTo(DeployBpmnData.class);
                eventManager.sendMessage(EventType.BUSINESS_REGISTER_FUNCTION, registerFunctionData);
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
    public Future<Void> destroyFunction(JsonObject data) {

        return Future.succeededFuture();
    }

    @Override
    public Future<Void> loaderClass(JsonObject data) {
        Promise<Void> respond = Promise.promise();
        LoaderClassRequest request = data.mapTo(LoaderClassRequest.class);
        businessExecute.executeBlocking(promise -> {
            // 下载jar文件
            try {
                // classLoaderManager.downloadFileFromUrl("http://localhost:9675/"+request.getJarName(),request.getJarName());
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

}
