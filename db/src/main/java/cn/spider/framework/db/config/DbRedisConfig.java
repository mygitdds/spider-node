package cn.spider.framework.db.config;

import io.vertx.core.Vertx;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.core.shareddata.SharedData;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import redis.clients.jedis.JedisPoolConfig;

/**
 * @BelongsProject: spider-node
 * @BelongsPackage: cn.spider.framework.db.config
 * @Author: dengdongsheng
 * @CreateTime: 2023-04-07  19:23
 * @Description: TODO
 * @Version: 1.0
 */
@Configuration
public class DbRedisConfig {

    public JedisPoolConfig buildJedisPoolConfig(Vertx vertx){
        SharedData sharedData = vertx.sharedData();
        LocalMap<String,String> localMap = sharedData.getLocalMap("config");
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMaxIdle(Integer.parseInt(localMap.get("redis-maxIdle")));
        jedisPoolConfig.setMinIdle(Integer.parseInt(localMap.get("redis-minIdle")));
        jedisPoolConfig.setMaxTotal(Integer.parseInt(localMap.get("redis-maxTotal")));
        jedisPoolConfig.setMaxWaitMillis(Long.parseLong(localMap.get("redis-maxWait")));
        jedisPoolConfig.setMinEvictableIdleTimeMillis(2000);
        jedisPoolConfig.setTestOnBorrow(true);
        jedisPoolConfig.setTestOnReturn(true);
        //Idle时进行连接扫描
        jedisPoolConfig.setTestWhileIdle(true);
        //表示idle object evitor两次扫描之间要sleep的毫秒数
        jedisPoolConfig.setTimeBetweenEvictionRunsMillis(30000);
        //表示idle object evitor每次扫描的最多的对象数
        jedisPoolConfig.setNumTestsPerEvictionRun(10);
        //表示一个对象至少停留在idle状态的最短时间，然后才能被idle object evitor扫描并驱逐；这一项只有在timeBetweenEvictionRunsMillis大于0时才有意义
        jedisPoolConfig.setMinEvictableIdleTimeMillis(200);

        return jedisPoolConfig;
    }
    @Bean
    public JedisConnectionFactory buildJedisConnectionFactory(Vertx vertx){
        SharedData sharedData = vertx.sharedData();
        LocalMap<String,String> localMap = sharedData.getLocalMap("config");
        JedisConnectionFactory jedisConnectionFactory = new JedisConnectionFactory();
        jedisConnectionFactory.setPoolConfig(buildJedisPoolConfig(vertx));
        jedisConnectionFactory.setHostName(localMap.get("redis-host-name"));
        jedisConnectionFactory.setPort(Integer.parseInt(localMap.get("redis-port")));
        if(localMap.containsKey("redis-password")){
            jedisConnectionFactory.setPassword(localMap.get("redis-password"));
        }
        jedisConnectionFactory.setTimeout(Integer.parseInt(localMap.get("redis-timeout")));
        jedisConnectionFactory.setUsePool(true);
        return jedisConnectionFactory;
    }
    @Bean("redisTemplate")
    public RedisTemplate<Object,Object> buildRedisTemplate(JedisConnectionFactory factory){
        RedisTemplate<Object,Object> redisTemplate = new RedisTemplate();
        redisTemplate.setConnectionFactory(factory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        redisTemplate.setEnableTransactionSupport(true);
        return redisTemplate;
    }
}
