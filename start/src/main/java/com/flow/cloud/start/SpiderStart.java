package com.flow.cloud.start;

import cn.spider.framework.common.role.BrokerRole;
import cn.spider.framework.linker.server.config.SpringConfig;
import com.flow.cloud.start.config.StartConfig;
import com.flow.cloud.start.util.ExceptionMessage;
import com.flow.cloud.start.util.PropertyReader;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.core.shareddata.SharedData;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.AbstractRefreshableApplicationContext;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Map;

import static com.flow.cloud.start.util.BannerHelper.banner;

/**
 * @Classname FlowStart
 * @Description spider-统一启动类
 * @Date 2021/10/22 23:55
 * @Created dds
 */
@Slf4j
public class SpiderStart {

    private static AbstractApplicationContext factory;

    public static Vertx vertxNew;

    public static void main(String[] args) {
        // 设置启动图案
        banner(1);
        // 设置集群类型
        ClusterManager clusterManager = new HazelcastClusterManager();
        VertxOptions options = new VertxOptions().setClusterManager(clusterManager);
        options.setWorkerPoolSize(10);

        // 加入集群
        Vertx.clusteredVertx(options, res -> {
            if (res.succeeded()) {
                Vertx vertx = res.result();
                // 蒋装载的信息放入 sharedData
                loadConfig(vertx);
                SharedData sharedData = vertx.sharedData();
                LocalMap<String, String> localMap = sharedData.getLocalMap("config");
                // 当为空的情况下，抛出异常推出程序
                if (localMap.isEmpty()) {
                    System.exit(1);
                }
                vertxNew = vertx;
                factory = new AnnotationConfigApplicationContext(StartConfig.class);

                DeploymentOptions deployOptions = new DeploymentOptions()
                        // verticle模式
                        .setWorker(true)
                        // 是否高可用
                        .setHa(true)
                        .setInstances(1);
                vertx.deployVerticle("cn.spider.framework.controller.ControllerVerticle", deployOptions, res1 -> {
                    if (res1.succeeded()) {
                        System.out.println("start-controller-启动成功");
                        // 循环启动 spider配置中的角色
                        for (String role : localMap.keySet()) {
                            switch (role) {
                                case "gateway":
                                    // 启动网关
                                    String gateway = "cn.spider.framework.gateway.GatewayVerticle";
                                    startRole(gateway, role, vertx, 1);
                                    break;
                                case "flow-node":
                                    String flowNode = "cn.spider.framework.flow.SpiderCoreVerticle";
                                    startRole(flowNode, role, vertx, 1);
                                    break;
                                case "scheduler":
                                    String linkerServer = "cn.spider.framework.linker.server.LinkerMainVerticle";
                                    startRole(linkerServer, role, vertx, 1);
                                    break;
                                case "transaction":
                                    String transactionCore = "cn.spider.framework.transaction.server.TransactionServerVerticle";
                                    startRole(transactionCore, role, vertx, 1);
                                    break;
                                case "log":
                                    String logPath = "cn.spider.framework.spider.log.es.LogVerticle";
                                    startRole(logPath, role, vertx, 1);
                            }
                        }
                    } else {
                        System.out.println("start-controller-fail"+res1.cause());
                        log.info("start-controller-fail");
                    }
                });
            } else {
                // failed!
                log.info("start-fail");
            }
        });
    }

    /**
     * 加载配置并且加入vertx的缓存中方便其他角色使用
     *
     * @param vertx
     */
    private static void loadConfig(Vertx vertx) {
        Map<String, String> spiderConf = PropertyReader.GetAllProperties("spiderConf.properties");
        SharedData sharedData = vertx.sharedData();
        LocalMap<String, String> localMap = sharedData.getLocalMap("config");
        localMap.putAll(spiderConf);
        localMap.put("flow-node", "1");
        localMap.put("scheduler", "1");
        localMap.put("transaction", "1");
        localMap.put("controller", "1");
        localMap.put("log", "1");


    }

    /**
     * 启动各个角色
     *
     * @param path
     * @param role
     * @param vertx
     * @param num
     */
    private static void startRole(String path, String role, Vertx vertx, int num) {
        DeploymentOptions deployOptions = new DeploymentOptions()
                // verticle模式
                .setWorker(true)
                // 是否高可用
                .setHa(true)
                .setInstances(num);
        vertx.deployVerticle(path, deployOptions, res1 -> {
            if (res1.succeeded()) {
                System.out.println("启动成功" + role);
                log.info("角色 {} 启动成功", role);
            } else {
                System.out.println("启动失败" + role + ExceptionMessage.getStackTrace(res1.cause()));
                log.info("角色 {} 启动失败,原因为 {}", role, ExceptionMessage.getStackTrace(res1.cause()));
            }
        });
    }

}