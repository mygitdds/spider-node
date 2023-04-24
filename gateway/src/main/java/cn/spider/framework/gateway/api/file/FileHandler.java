package cn.spider.framework.gateway.api.file;
import cn.spider.framework.container.sdk.interfaces.ContainerService;
import cn.spider.framework.db.list.RedisList;
import cn.spider.framework.db.map.RedisMap;
import io.vertx.core.Future;
import io.vertx.ext.web.Router;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Resource
    private RedisTemplate redisTemplate;

    private RedisList jarRedisList;

    private RedisList bpmnRedisList;

    private String path = ClassUtils.getDefaultClassLoader().getResource("").getPath() + "file";

    public void init(Router router) {
        this.router = router;
        jarRedisList = new RedisList(redisTemplate,"jar");
        bpmnRedisList = new RedisList(redisTemplate,"bpmn");
        uploadJar();
        uploadBpmn();
    }

    /**
     * 上传 jar文件到该路径
     */
    public void uploadJar() {
        router.post("/upload/jar")
                .handler(ctx -> {
                    ctx.end();
                    ctx.request().setExpectMultipart(true);
                    ctx.request().uploadHandler((upload) -> {
                        String uploadedFileName = (new File(path, upload.filename())).getPath();
                        Future<Void> fut = upload.streamToFileSystem(uploadedFileName);
                        fut.onSuccess(suss -> {
                            log.info("文件名称 {} 上传成功", upload.filename());
                            System.out.println("文件名称"+upload.filename()+"上传成功");
                            // step1 保存名称-
                            jarRedisList.addQueue(upload.filename());
                        }).onFailure(fail -> {
                            System.out.println("文件名称"+upload.filename()+"上传失败"+fail);
                            log.info("文件名称 {} 上传失败", upload.filename());
                        });
                        // 解析出来所有的taskComponent,taskServer// 返回前端
                    });
                });
    }

    public void uploadBpmn() {
        router.post("/upload/bpmn")
                .handler(ctx -> {
                    ctx.end();
                    ctx.request().setExpectMultipart(true);
                    ctx.request().uploadHandler((upload) -> {
                        String uploadedFileName = (new File(path, upload.filename())).getPath();
                        Future<Void> fut = upload.streamToFileSystem(uploadedFileName);
                        fut.onSuccess(suss -> {
                            bpmnRedisList.addQueue(upload.filename());
                            System.out.println("文件名称"+upload.filename()+"上传成功");
                        }).onFailure(fail -> {
                            System.out.println("文件名称"+upload.filename()+"上传失败"+fail);
                            log.info("文件名称 {} 上传失败", upload.filename());
                        });
                        // 解析出来所有的taskComponent,taskServer// 返回前端
                    });
                });
    }

}
