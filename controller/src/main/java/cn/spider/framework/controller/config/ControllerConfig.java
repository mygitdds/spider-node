package cn.spider.framework.controller.config;

import cn.spider.framework.common.event.EventConfig;
import cn.spider.framework.common.event.EventManager;
import cn.spider.framework.controller.ControllerVerticle;
import cn.spider.framework.controller.election.ElectionLeader;
import cn.spider.framework.controller.follower.FollowerManager;
import cn.spider.framework.controller.leader.LeaderManager;
import cn.spider.framework.controller.sdk.interfaces.LeaderHeartService;
import cn.spider.framework.db.config.DbRedisConfig;
import cn.spider.framework.db.config.RedisConfig;
import io.vertx.core.Vertx;
import org.springframework.context.ApplicationContext;
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
@Import({RedisConfig.class, EventConfig.class})
@Configuration
public class ControllerConfig {
    @Bean
    public Vertx buildVertx() {
        return ControllerVerticle.clusterVertx;
    }

    @Bean
    public FollowerManager buildFollowerManager(Vertx vertx, RedisTemplate redisTemplate,LeaderHeartService leaderHeartService) {
        return new FollowerManager(vertx, redisTemplate,leaderHeartService);
    }

    @Bean
    public LeaderManager buildLeaderManager(EventManager eventManager, Vertx vertx) {
        return new LeaderManager(eventManager, vertx);
    }



    @Bean
    public ElectionLeader buildElectionLeader(Vertx vertx, RedisTemplate redisTemplate, FollowerManager followerManager, LeaderManager leaderManager) {
        return new ElectionLeader(vertx, redisTemplate, leaderManager, followerManager);
    }

    @Bean
    public LeaderHeartService build(Vertx vertx) {
        String addr = LeaderHeartService.ADDRESS;
        return LeaderHeartService.createProxy(vertx, addr);
    }




}
