package cn.spider.framework.flow.funtion;

import cn.spider.framework.container.sdk.data.LoaderClassRequest;
import cn.spider.framework.db.list.RedisList;
import cn.spider.framework.flow.load.loader.ClassLoaderManager;
import com.alibaba.fastjson.JSON;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.List;
import java.util.Set;

/**
 * @BelongsProject: spider-node
 * @BelongsPackage: cn.spider.framework.flow.funtion
 * @Author: dengdongsheng
 * @CreateTime: 2023-04-14  01:46
 * @Description: TODO
 * @Version: 1.0
 */
@Slf4j
@Component
public class InitLoaderClassService {
    @Resource
    private ClassLoaderManager classLoaderManager;

    @Resource
    private RedisTemplate redisTemplate;

    private RedisList jarRedisList;

    @PostConstruct
    public void init() {
        this.jarRedisList = new RedisList(redisTemplate, "loaderJar");
        loaderClass();
    }

    public void loaderClass() {
        List<String> jars = this.jarRedisList.queueData();
        Set<String> jarsSet = Sets.newHashSet(jars);
        jarsSet.forEach(item -> {
            LoaderClassRequest request = JSON.parseObject(item, LoaderClassRequest.class);
            classLoaderManager.loaderUrlJar(request.getJarName(), request.getClassPath());
            log.info("加载成功"+item);
        });
    }
}
