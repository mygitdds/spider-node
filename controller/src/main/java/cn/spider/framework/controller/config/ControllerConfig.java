package cn.spider.framework.controller.config;

import cn.spider.framework.common.event.EventConfig;
import cn.spider.framework.common.event.EventManager;
import cn.spider.framework.controller.ControllerVerticle;
import cn.spider.framework.controller.election.ElectionLeader;
import cn.spider.framework.controller.follower.FollowerManager;
import cn.spider.framework.controller.leader.LeaderManager;
import cn.spider.framework.db.config.DbRedisConfig;
import io.vertx.core.Vertx;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * @BelongsProject: spider-node
 * @BelongsPackage: cn.spider.framework.controller.config
 * @Author: dengdongsheng
 * @CreateTime: 2023-04-20  16:34
 * @Description:
 * @Version: 1.0
 */
@Import({DbRedisConfig.class, EventConfig.class})
@Configuration
public class ControllerConfig {
    @Bean
    public Vertx buildVertx(){
        return ControllerVerticle.clusterVertx;
    }

    @Bean
    public FollowerManager buildFollowerManager(Vertx vertx, RedisTemplate redisTemplate){
        return new FollowerManager(vertx,redisTemplate);
    }

    @Bean
    public LeaderManager buildLeaderManager(EventManager eventManager,Vertx vertx){
        return new LeaderManager(eventManager,vertx);
    }

    @Bean
    public ElectionLeader buildElectionLeader(Vertx vertx,RedisTemplate redisTemplate,FollowerManager followerManager,LeaderManager leaderManager){
        return new ElectionLeader(vertx,redisTemplate,leaderManager,followerManager);
    }

}
