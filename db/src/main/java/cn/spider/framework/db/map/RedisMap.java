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

    private WorkerExecutor workerExecutor;

    public RedisMap(RedisTemplate redisEnv, String preKey, WorkerExecutor workerExecutor) {
        this.redisEnv = redisEnv;
        this.preKey = preKey;
        this.workerExecutor = workerExecutor;
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
        // 交给线程池区做
        workerExecutor.executeBlocking(promise -> {
            try {
                redisEnv.opsForHash().put(preKey, key, value);
                promise.complete();
            } catch (Exception e) {
                promise.fail(e);
            }
        }).onSuccess(suss -> {
            log.info("key {} ,value {} put suss", key, value);
            // 打印相应日志
        }).onFailure(fail -> {
            // 打印想要日志
            log.error("key {} ,value {} put fail {}", key, ExceptionMessage.getStackTrace(fail));
        });

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
        workerExecutor.executeBlocking(promise -> {
            try {
                if (in != null && in.size() > 0) {
                    Map<String, String> kvMap = new HashMap<>(in.size());
                    kvMap.forEach((key, val) -> kvMap.put(key, serializeObject(val)));
                    redisEnv.opsForHash().putAll(preKey, kvMap);
                }
                promise.complete();
            } catch (Exception e) {
                promise.fail(e);
            }

        }).onSuccess(suss -> {
            log.info("putAll {} suss", JSON.toJSONString(in));
            // 打印相应日志
        }).onFailure(fail -> {
            // 打印想要日志
            log.error("putAll {} fail", ExceptionMessage.getStackTrace(fail));
        });
    }

    /**
     * remove
     *
     * @param key key whose mapping is to be removed from the map
     * @return
     */
    public Object remove(Object key) {
        // 交给线程池去做
        workerExecutor.executeBlocking(promise -> {
            try {
                int size = 1;
                Object[] hasKeys = new Object[size];
                for (int i = 0; i < size; i++) {
                    hasKeys[i] = key;
                }
                redisEnv.opsForHash().delete(preKey, hasKeys);
                promise.complete();
            } catch (Exception e) {
                promise.fail(e);
            }
        }).onSuccess(suss -> {
            log.info("key {} remove suss", key);
            // 打印相应日志
        }).onFailure(fail -> {
            // 打印想要日志
            log.info("key {} remove fail {}", key, ExceptionMessage.getStackTrace(fail));
        });


        return true;
    }

    /**
     * 删除
     */
    public void clear() {
        // 交给线程池去做
        workerExecutor.executeBlocking(promise -> {
            try {
                redisEnv.opsForHash().delete(preKey);
            } catch (Exception e) {
                promise.fail(e);
            }
        }).onSuccess(suss -> {
            log.info("clear {} suss", preKey);
            // 打印相应日志
        }).onFailure(fail -> {
            // 打印想要日志
            log.error("clear {} fail {}", preKey, ExceptionMessage.getStackTrace(fail));
        });


    }

    public Map<String, String> getHash() {
        return redisEnv.opsForHash().entries(preKey);
    }

}
