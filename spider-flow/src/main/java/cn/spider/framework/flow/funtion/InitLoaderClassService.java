package cn.spider.framework.flow.funtion;

import cn.spider.framework.common.config.Constant;
import cn.spider.framework.common.data.enums.JarStatus;
import cn.spider.framework.container.sdk.data.LoaderClassRequest;
import cn.spider.framework.db.list.RedisList;
import cn.spider.framework.db.map.RedisMap;
import cn.spider.framework.flow.load.loader.ClassLoaderManager;
import com.alibaba.fastjson.JSON;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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


    private RedisMap jarRedisMap;

    @PostConstruct
    public void init() {
        this.jarRedisMap = new RedisMap(redisTemplate, Constant.JAR_KEY);
        loaderClass();
    }

    public void loaderClass() {
        Map<String,String> jarMap = jarRedisMap.getHash();
        jarMap.forEach((key,value)->{
            LoaderClassRequest request = JSON.parseObject(value, LoaderClassRequest.class);
            if(Objects.isNull(request.getStatus()) || request.getStatus().equals(JarStatus.STOP)){
                return;
            }
            try {
                classLoaderManager.loaderUrlJar(request.getJarName(), request.getClassPath(),request.getUrl());
            } catch (Exception e) {
                log.error("加载失败的 jar {}",key);
            }
            log.info("加载成功"+key);
        });
    }
}
