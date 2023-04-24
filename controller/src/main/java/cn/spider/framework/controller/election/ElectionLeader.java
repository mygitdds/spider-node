package cn.spider.framework.controller.election;

import cn.spider.framework.common.config.Constant;
import cn.spider.framework.common.role.BrokerRole;
import cn.spider.framework.common.utils.BrokerInfoUtil;
import cn.spider.framework.controller.follower.FollowerManager;
import cn.spider.framework.controller.leader.LeaderManager;
import io.vertx.core.Vertx;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * @BelongsProject: spider-node
 * @BelongsPackage: cn.spider.framework.controller.election
 * @Author: dengdongsheng
 * @CreateTime: 2023-04-20  15:33
 * @Description: 选举leader
 * @Version: 1.0
 */
public class ElectionLeader {

    // 发起选举
    private RedisTemplate<String,String> redisTemplate;

    private LeaderManager leaderManager;

    private FollowerManager followerManager;

    private Vertx vertx;

    public ElectionLeader(Vertx vertx, RedisTemplate redisTemplate, LeaderManager leaderManager, FollowerManager followerManager) {
        this.redisTemplate = redisTemplate;
        this.vertx = vertx;
        this.leaderManager = leaderManager;
        this.followerManager = followerManager;
    }

    /**
     * 进行选择
     */
    public void election() {
        BrokerRole role = buildBrokerRole();
        switch (role) {
            case LEADER:
                leaderManager.init();
                break;
            case FOLLOWER:
                // 启动follower
                followerManager.init();
                break;
        }
    }


    // 获取角色
    private BrokerRole buildBrokerRole() {
        String brokerName = BrokerInfoUtil.queryBrokerName(this.vertx);
        boolean result = this.redisTemplate.opsForValue()
                .setIfAbsent(Constant.LEADER_CONFIG_KEY, brokerName);
        if(!result){
            String leaderBrokerName = this.redisTemplate.opsForValue().get(Constant.LEADER_CONFIG_KEY);
            result = StringUtils.equals(leaderBrokerName,brokerName);
        }
        return result ? BrokerRole.LEADER : BrokerRole.FOLLOWER;
    }
}
