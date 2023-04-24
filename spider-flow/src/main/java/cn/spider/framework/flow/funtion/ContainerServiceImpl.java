package cn.spider.framework.flow.funtion;

import cn.spider.framework.common.utils.ExceptionMessage;
import cn.spider.framework.container.sdk.data.LoaderClassRequest;
import cn.spider.framework.container.sdk.data.RegisterFunctionRequest;
import cn.spider.framework.container.sdk.interfaces.ContainerService;
import cn.spider.framework.flow.load.loader.ClassLoaderManager;
import cn.spider.framework.flow.resource.factory.StartEventFactory;
import cn.spider.framework.flow.sync.Constant;
import cn.spider.framework.flow.sync.Publish;
import cn.spider.framework.flow.sync.SyncBusinessRecord;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.WorkerExecutor;
import io.vertx.core.json.JsonObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.Executor;

/**
 * @BelongsProject: spider-node
 * @BelongsPackage: cn.spider.framework.flow.funtion
 * @Author: dengdongsheng
 * @CreateTime: 2023-03-26  18:07
 * @Description: 功能生命周期管理实习类
 * @Version: 1.0
 */
@Component
public class ContainerServiceImpl implements ContainerService {

    @Resource
    private StartEventFactory startEventFactory;

    @Resource
    private WorkerExecutor businessExecute;

    @Resource
    private ClassLoaderManager classLoaderManager;

    @Resource
    private Publish publish;

    /**
     * 开启功能-可执行
     * @param data
     * @return
     */
    @Override
    public Future<Void> registerFunction(JsonObject data) {
        Promise<Void> promise = Promise.promise();
        businessExecute.executeBlocking(promises->{
            try {
                RegisterFunctionRequest request = data.mapTo(RegisterFunctionRequest.class);
                startEventFactory.dynamicsLoaderBpmn(request.getBpmnName());
                // leader才会同步成功-》follower进行消费
                publish.push(Constant.BUSINESS_REGISTER_FUNCTION,data.toString());
                promises.complete();
            } catch (Exception e) {
                System.out.println(ExceptionMessage.getStackTrace(e));
                promises.fail(e);
            }
        }).onSuccess(suss->{
            promise.complete();
            // 进行同步
        }).onFailure(fail->{
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
                classLoaderManager.loaderUrlJar(request.getJarName(),request.getClassPath());
                // 进行同步
                publish.push(Constant.LOADER_JAR,data.toString());
                promise.complete();
            } catch (Exception e) {
                System.out.println(ExceptionMessage.getStackTrace(e));
                promise.fail(e);
            }
        }).onSuccess(suss->{
            respond.complete();
        }).onFailure(fail->{
            respond.fail(fail);
        });
        return respond.future();
    }

}
