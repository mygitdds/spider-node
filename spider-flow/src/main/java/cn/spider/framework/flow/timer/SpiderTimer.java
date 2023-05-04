package cn.spider.framework.flow.timer;

import cn.spider.framework.flow.engine.StoryEngine;
import com.google.common.collect.Maps;
import io.vertx.core.Vertx;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Map;

/**
 * @BelongsProject: spider-node
 * @BelongsPackage: cn.spider.framework.flow.timer
 * @Author: dengdongsheng
 * @CreateTime: 2023-04-27  18:27
 * @Description: TODO
 * @Version: 1.0
 */
@Component
public class SpiderTimer {

    @Resource
    private Vertx vertx;

    @Resource
    private StoryEngine storyEngine;

    @Resource
    private RedisTemplate redisTemplate;

    /**
     * 注册延迟 一秒延迟（这个地方可以做适当调整）
     * @param brokerName
     */
    public void becomeTimer(String brokerName){
        vertx.setTimer(2000, id -> {
            storyEngine.getFlowExampleManager().become(brokerName);
        });
    }

    public void deleteElectionKey(String key){
        vertx.setTimer(2000*20, id -> {
            redisTemplate.delete(key);
        });
    }
}
