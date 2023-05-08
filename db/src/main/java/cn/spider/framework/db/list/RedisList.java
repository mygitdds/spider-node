package cn.spider.framework.db.list;
import org.springframework.data.redis.core.RedisTemplate;
import java.util.List;

/**
 * @BelongsProject: spider-node
 * @BelongsPackage: cn.spider.framework.db.list
 * @Author: dengdongsheng
 * @CreateTime: 2023-03-26  13:40
 * @Description: redis的list结构
 * @Version: 1.0
 */
public class RedisList {

    protected RedisTemplate redisTemplate;

    /**
     * list-key
     */
    private String preKey;

    public RedisList(RedisTemplate redisTemplate, String preKey) {
        this.redisTemplate = redisTemplate;
        this.preKey = preKey;
    }

    /**
     * 加入队伍
     * @param value
     */
    public void addQueue(String value) {
        // 先进行移除
        leaveQueue(value);
        redisTemplate.opsForList().rightPush(preKey,value);
    }

    /**
     * 队伍详情
     */
    public List<String> queueData() {
        List<String> result = redisTemplate.opsForList().range(preKey, 0, -1);
        return result;
    }

    /**
     * 离开队伍
     * @param value
     */
    public void leaveQueue(String value) {
        redisTemplate.opsForList().remove(preKey, 0, value);
    }

    public void clear(){
        redisTemplate.delete(preKey);
    }
}
