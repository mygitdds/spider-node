package cn.spider.framework.flow.load.loader;

import cn.spider.framework.annotation.TaskComponent;
import cn.spider.framework.common.utils.SpringUtil;
import cn.spider.framework.flow.container.component.TaskComponentManager;
import cn.spider.framework.flow.engine.scheduler.SchedulerManager;
import cn.spider.framework.flow.engine.scheduler.data.ComponentInfo;
import cn.spider.framework.flow.load.proxy.TaskServiceProxy;
import cn.spider.framework.flow.load.proxy.factory.TaskServiceProxyFactory;
import cn.spider.framework.linker.sdk.interfaces.LinkerService;
import com.alibaba.fastjson.JSON;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ClassUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * @BelongsProject: spider-node
 * @BelongsPackage: cn.spider.framework.flow.load.loader
 * @Author: dengdongsheng
 * @CreateTime: 2023-03-21  03:01
 * @Description: class加载管理器
 * @Version: 1.0
 */
public class ClassLoaderManager {

    private TaskComponentManager taskComponentManager;

    private Map<String, AppointClassLoader> classLoaderMap;

    /**
     * 根据类路径隐射class对象
     */
    private Map<String,ClassLoader> classMap;

    private SchedulerManager schedulerManager;

    private String path = ClassUtils.getDefaultClassLoader().getResource("").getPath() + "jar/";

    public void init(TaskComponentManager taskComponentManager,SchedulerManager schedulerManager) {
        this.classLoaderMap = new HashMap<>();
        this.taskComponentManager = taskComponentManager;
        this.schedulerManager = schedulerManager;
        this.classMap = new HashMap<>();
    }


    /**
     * 指定加载对于包下的class对象
     *
     * @param name
     * @param classPath
     */
    public void loaderUrlJar(String name, String classPath) {
        try {
            AppointClassLoader oldLoader = classLoaderMap.get(name);
            if (Objects.nonNull(oldLoader)) {
                // 卸载class对象
                oldLoader.unloadJar();
                // map进入卸载
                classLoaderMap.remove(name);
                // 通过-schedulerManager进行卸载
            }
            // step1: 构造url -- 从配置中获取（后续改造）
            URL jar = new URL("jar:http://localhost:9675/" + name+"!/");
            // step2: 创建 appointClassLoader
            AppointClassLoader appointClassLoader = new AppointClassLoader(jar,this.getClass().getClassLoader());
            // step3: 进行加载替换
            Set<Class> classes = appointClassLoader.loadClassNew(classPath);
            // 打印下，加载了那些文件
            // step4: 对taskService重新装载- 便于正确的调度
            classLoaderMap.put(name, appointClassLoader);
            // step5: 循环加载到spider-flow
            classes.forEach(item -> {
                System.out.println("加载成功的class"+item.getTypeName());
                this.classMap.put(item.getTypeName(),item.getClassLoader());
                // 在spider中卸载 对于的代理对象
                if(!item.isInterface()){
                    return;
                }
                taskComponentManager.unload(item);
                TaskComponent taskComponent = AnnotationUtils.findAnnotation(item, TaskComponent.class);
                // 进行注册到spider-flow中进行代理
                taskComponentManager.appointLoaderTaskServer(item);
                schedulerManager.addClass(taskComponent.name(),taskComponent.workerName());
            });
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }


    public void downloadFileFromUrl(String fileUrl, String fileName) throws Exception {
        URL url = new URL(fileUrl);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setConnectTimeout(6000);
        urlConnection.setReadTimeout(6000);
        if (urlConnection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            throw new RuntimeException("文件读取失败");
        }
        InputStream inputStream = urlConnection.getInputStream();
        byte[] buffer = new byte[1024];
        int len;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        while ((len = inputStream.read(buffer)) != -1) {
            byteArrayOutputStream.write(buffer, 0, len);
        }
        byteArrayOutputStream.close();
        // 读取配置的文件地址
        File file = new File(path + fileName);
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        fileOutputStream.write(byteArrayOutputStream.toByteArray());
        fileOutputStream.close();
        inputStream.close();
        System.out.println("下载成功：" + System.getProperty("java.io.tmpdir") + fileName);
    }

    public ClassLoader queryClassLoader(String classType){
        return this.classMap.get(classType);
    }

}
