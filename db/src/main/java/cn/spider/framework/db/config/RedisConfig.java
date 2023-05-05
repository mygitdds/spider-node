package cn.spider.framework.db.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import io.vertx.core.Vertx;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.core.shareddata.SharedData;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * @BelongsProject: spider-node
 * @BelongsPackage: cn.spider.framework.db.config
 * @Author: dengdongsheng
 * @CreateTime: 2023-05-05  16:38
 * @Description: TODO
 * @Version: 1.0
 */
@Configuration
public class RedisConfig {
    /**
     * retemplate相关配置
     */
    @Bean("redisTemplate")
    public RedisTemplate<String, Object> redisTemplate(LettuceConnectionFactory factory) {

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        // 配置连接工厂
        template.setConnectionFactory(factory);

        //使用Jackson2JsonRedisSerializer来序列化和反序列化redis的value值（默认使用JDK的序列化方式）
        Jackson2JsonRedisSerializer jacksonSeial = new Jackson2JsonRedisSerializer(Object.class);

        ObjectMapper om = new ObjectMapper();
        // 指定要序列化的域，field,get和set,以及修饰符范围，ANY是都有包括private和public
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        // 指定序列化输入的类型，类必须是非final修饰的，final修饰的类，比如String,Integer等会跑出异常
        om.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        jacksonSeial.setObjectMapper(om);

        // 值采用json序列化
        template.setValueSerializer(jacksonSeial);
        //使用StringRedisSerializer来序列化和反序列化redis的key值
        template.setKeySerializer(new StringRedisSerializer());

        // 设置hash key 和value序列化模式
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(jacksonSeial);
        template.afterPropertiesSet();

        return template;
    }

    @Bean
    public DefaultClientResources lettuceClientResources() {
        return DefaultClientResources.create();
    }

    @Bean("lettuceConnectionFactory")
    public LettuceConnectionFactory lettuceConnectionFactory(Vertx vertx, ClientResources clientResources, GenericObjectPoolConfig redisPoolConfig) {

        ClusterTopologyRefreshOptions topologyRefreshOptions = ClusterTopologyRefreshOptions.builder()
                //按照周期刷新拓扑
                .enablePeriodicRefresh(Duration.ofSeconds(10))
                //根据事件刷新拓扑
                .enableAllAdaptiveRefreshTriggers()
                .build();

        SharedData sharedData = vertx.sharedData();
        LocalMap<String, String> localMap = sharedData.getLocalMap("config");

        ClusterClientOptions clusterClientOptions = ClusterClientOptions.builder()
                //redis命令超时时间,超时后才会使用新的拓扑信息重新建立连接
                .autoReconnect(true)
                .cancelCommandsOnReconnectFailure(false)
                .disconnectedBehavior(ClientOptions.DisconnectedBehavior.DEFAULT)
                .pingBeforeActivateConnection(true)
                .timeoutOptions(TimeoutOptions.enabled(Duration.ofSeconds(Integer.parseInt(localMap.get("redis-timeout")))))
                .topologyRefreshOptions(topologyRefreshOptions)
                .socketOptions(SocketOptions.builder().connectTimeout(Duration.ofSeconds(Integer.parseInt(localMap.get("redis-timeout")))).keepAlive(true).build())
                .build();

        LettuceClientConfiguration clientConfig = LettucePoolingClientConfiguration.builder()
                .clientResources(clientResources)
                .clientOptions(clusterClientOptions)
                .commandTimeout(Duration.ofSeconds(Integer.parseInt(localMap.get("redis-timeout"))))
                .poolConfig(redisPoolConfig)
                .build();


        //单机
        RedisStandaloneConfiguration redisConfiguration = new RedisStandaloneConfiguration(localMap.get("redis-host-name"), Integer.parseInt(localMap.get("redis-port")));
        if(localMap.containsKey("redis-password")){
            redisConfiguration.setPassword(localMap.get("redis-password"));
        }


        LettuceConnectionFactory lettuceConnectionFactory = new LettuceConnectionFactory(redisConfiguration, clientConfig);
        lettuceConnectionFactory.afterPropertiesSet();
        lettuceConnectionFactory.setValidateConnection(false);

        return lettuceConnectionFactory;

    }

    /**
     * Redis连接池配置</b>
     */
    @Bean
    public GenericObjectPoolConfig redisPoolConfig(Vertx vertx) {
        GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
        SharedData sharedData = vertx.sharedData();
        LocalMap<String, String> localMap = sharedData.getLocalMap("config");
        poolConfig.setMaxIdle(Integer.parseInt(localMap.get("redis-maxIdle")));
        poolConfig.setMinIdle(Integer.parseInt(localMap.get("redis-minIdle")));
        poolConfig.setMaxTotal(Integer.parseInt(localMap.get("redis-maxTotal")));
        poolConfig.setMaxWaitMillis(Long.parseLong(localMap.get("redis-maxWait")));
        poolConfig.setTimeBetweenEvictionRunsMillis(100);
        return poolConfig;
    }
}
