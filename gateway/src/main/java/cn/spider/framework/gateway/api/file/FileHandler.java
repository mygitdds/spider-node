package cn.spider.framework.gateway.api.file;
import cn.spider.framework.common.config.Constant;
import cn.spider.framework.common.data.UploadBpmnParam;
import cn.spider.framework.common.data.enums.JarStatus;
import cn.spider.framework.container.sdk.data.LoaderClassRequest;
import cn.spider.framework.db.list.RedisList;
import cn.spider.framework.db.map.RedisMap;
import cn.spider.framework.gateway.common.ResponseData;
import com.alibaba.fastjson.JSON;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

import javax.annotation.Resource;
import java.io.File;

/**
 * @BelongsProject: spider-node
 * @BelongsPackage: cn.spider.framework.gateway.api.file
 * @Author: dengdongsheng
 * @CreateTime: 2023-03-15  13:43
 * @Description: 文件的api交互
 * @Version: 1.0
 */
@Slf4j
@Component
public class FileHandler {

    private Router router;

    private String path = ClassUtils.getDefaultClassLoader().getResource("").getPath() + "file";

    public void init(Router router) {
        this.router = router;
        uploadJar();
        uploadBpmn();
    }



    /**
     * 上传 jar文件到该路径
     */
    public void uploadJar() {
        router.post("/upload/jar")
                .handler(ctx -> {
                    ctx.request().setExpectMultipart(true);
                    ctx.request().uploadHandler((upload) -> {
                        HttpServerResponse response = ctx.response();
                        response.putHeader("content-type", "application/json");
                        String uploadedFileName = (new File(path, upload.filename())).getPath();
                        Future<Void> fut = upload.streamToFileSystem(uploadedFileName);
                        fut.onSuccess(suss -> {
                            log.info("文件名称 {} 上传成功", upload.filename());
                            // step1 保存名称-
                            response.end(ResponseData.suss());
                        }).onFailure(fail -> {
                            log.info("文件名称 {} 上传失败", upload.filename());
                            response.end(ResponseData.fail(fail));
                        });
                    });
                });
    }



    public void uploadBpmn() {
        router.post("/upload/bpmn")
                .handler(ctx -> {
                    ctx.request().setExpectMultipart(true);
                    ctx.request().uploadHandler((upload) -> {
                        String uploadedFileName = (new File(path, upload.filename())).getPath();
                        Future<Void> fut = upload.streamToFileSystem(uploadedFileName);
                        HttpServerResponse response = ctx.response();
                        response.putHeader("content-type", "application/json");
                        fut.onSuccess(suss -> {
                            response.end(ResponseData.suss());
                        }).onFailure(fail -> {
                            log.error("文件名称 {} 上传失败", upload.filename());
                            response.end(ResponseData.fail(fail));
                        });

                    });
                });
    }

}
