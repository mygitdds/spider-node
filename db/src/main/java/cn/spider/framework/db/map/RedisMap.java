package cn.spider.framework.db.map;

import cn.spider.framework.common.utils.ExceptionMessage;
import com.alibaba.fastjson.JSON;
import io.vertx.core.WorkerExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * @BelongsProject: spider-node
 * @BelongsPackage: cn.spider.framework.db.map
 * @Author: dengdongsheng
 * @CreateTime: 2023-03-20  00:33
 * @Description: TODO
 * @Version: 1.0
 */
@Slf4j
public class RedisMap {
    protected RedisTemplate redisEnv;

    /**
     * 每个请求的key表示
     */
    private String preKey;

    public RedisMap(RedisTemplate redisEnv, String preKey) {
        this.redisEnv = redisEnv;
        this.preKey = preKey;
    }

    /**
     * 获取数据
     *
     * @param key the key whose associated value is to be returned
     * @return
     */
    public Object get(Object key) {
        return redisEnv.opsForHash().get(preKey, key);
    }


    /**
     * 插入hashmap中并且写入redis
     *
     * @param key   key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return
     */
    public Object put(Object key, Object value) {
        redisEnv.opsForHash().put(preKey, key, value);
        return null;
    }


    private String serializeObject(Object object) {
        return JSON.toJSONString(object);
    }

    /**
     * 批量插入
     *
     * @param in mappings to be stored in this map
     */
    public void putAll(Map in) {
        // 交给线程池去做
        try {
            if (in != null && in.size() > 0) {
                Map<String, String> kvMap = new HashMap<>(in.size());
                kvMap.forEach((key, val) -> kvMap.put(key, serializeObject(val)));
                redisEnv.opsForHash().putAll(preKey, kvMap);
            }
        } catch (Exception e) {
            log.error("存储-redis-fail {}", ExceptionMessage.getStackTrace(e));
        }
    }

    /**
     * remove
     *
     * @param key key whose mapping is to be removed from the map
     * @return
     */
    public Object remove(Object key) {
        // 交给线程池去做
        try {
            int size = 1;
            Object[] hasKeys = new Object[size];
            for (int i = 0; i < size; i++) {
                hasKeys[i] = key;
            }
            redisEnv.opsForHash().delete(preKey, hasKeys);
        } catch (Exception e) {
            log.error("delete-redis-fail {}", ExceptionMessage.getStackTrace(e));
        }
        return true;
    }

    /**
     * 删除
     */
    public void clear() {
        // 交给线程池去做
        redisEnv.opsForHash().delete(preKey);
    }

    public Map<String, String> getHash() {
        return redisEnv.opsForHash().entries(preKey);
    }


    public Map<String, Object> getHashObject() {
        return redisEnv.opsForHash().entries(preKey);
    }



}
