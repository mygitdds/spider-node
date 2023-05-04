package cn.spider.framework.gateway.config;

import cn.spider.framework.common.role.BrokerRole;
import cn.spider.framework.container.sdk.interfaces.BusinessService;
import cn.spider.framework.container.sdk.interfaces.ContainerService;
import cn.spider.framework.container.sdk.interfaces.FlowService;
import cn.spider.framework.db.config.DbRedisConfig;
import cn.spider.framework.gateway.GatewayVerticle;
import io.vertx.core.Vertx;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @BelongsProject: spider-node
 * @BelongsPackage: cn.spider.framework.gateway.config
 * @Author: dengdongsheng
 * @CreateTime: 2023-03-19  14:57
 * @Description: spring组件的配置类
 * @Version: 1.0
 */
@Configuration
@Import({DbRedisConfig.class})
@ComponentScan(basePackages = {"cn.spider.framework.gateway.api.*"})
public class SpringConfig {

    @Bean
    public Vertx buildVertx(){
        return GatewayVerticle.clusterVertx;
    }

    /**
     * 请求到leader
     * @param vertx
     * @return
     */
    @Bean
    public ContainerService buildContainerService(Vertx vertx){
        return ContainerService.createProxy(vertx, BrokerRole.LEADER.name()+ContainerService.ADDRESS);
    }

    /**
     * 请求所有节点
     * @param vertx
     * @return
     */
    @Bean
    public FlowService buildFlowService(Vertx vertx){
       return FlowService.createProxy(vertx,FlowService.ADDRESS);
    }

    /**
     * 只发送到leader
     * @param vertx
     * @return
     */
    @Bean
    public BusinessService buildBusinessService(Vertx vertx){
        return BusinessService.createProxy(vertx,BrokerRole.LEADER.name()+BusinessService.ADDRESS);
    }

}
