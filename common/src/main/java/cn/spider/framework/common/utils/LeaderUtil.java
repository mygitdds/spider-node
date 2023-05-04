package cn.spider.framework.common.utils;

import cn.spider.framework.common.config.Constant;
import cn.spider.framework.common.role.BrokerRole;
import io.vertx.core.Vertx;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * @BelongsProject: spider-node
 * @BelongsPackage: cn.spider.framework.common.utils
 * @Author: dengdongsheng
 * @CreateTime: 2023-04-21  14:01
 * @Description: 获取当前节点是什么角色
 * @Version: 1.0
 */
public class LeaderUtil {
    public static BrokerRole queryBrokerRole(Vertx vertx, ApplicationContext applicationContext) {
        RedisTemplate<String,String> redisTemplate = applicationContext.getBean(RedisTemplate.class);
        String leaderName = redisTemplate.opsForValue().get(Constant.LEADER_CONFIG_KEY);
        String brokerName = BrokerInfoUtil.queryBrokerName(vertx);
        return StringUtils.equals(leaderName, brokerName) ? BrokerRole.LEADER : BrokerRole.FOLLOWER;
    }
}
