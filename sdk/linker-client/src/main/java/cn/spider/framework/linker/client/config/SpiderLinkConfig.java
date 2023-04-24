package cn.spider.framework.linker.client.config;

import cn.spider.framework.linker.client.grpc.TransferServerHandler;
import cn.spider.framework.linker.client.socket.SocketManager;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @BelongsProject: spider-node
 * @BelongsPackage: cn.spider.framework.linker.client.config
 * @Author: dengdongsheng
 * @CreateTime: 2023-03-27  15:13
 * @Description: TODO
 * @Version: 1.0
 */
public class SpiderLinkConfig {

    @Bean
    public Vertx buildVertx(){
        VertxOptions options = new VertxOptions();
        options.setWorkerPoolSize(10);
        return Vertx.vertx(options);
    }
    @Bean
    public TransferServerHandler buildTransferServerHandler(Vertx vertx,
                                                            ApplicationContext applicationContext,
                                                            Executor spiderTaskPool,
                                                            PlatformTransactionManager platformTransactionManager,
                                                            TransactionDefinition transactionDefinition){
        TransferServerHandler transferServerHandler = new TransferServerHandler();
        transferServerHandler.init(vertx,applicationContext,spiderTaskPool,platformTransactionManager,transactionDefinition);
        return transferServerHandler;
    }

    @Bean(name = "spiderTaskPool")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        //核心线程池大小
        executor.setCorePoolSize(8);
        //最大线程数
        executor.setMaxPoolSize(20);
        //队列容量
        executor.setQueueCapacity(4000);
        //活跃时间
        executor.setKeepAliveSeconds(200);
        //线程名字前缀
        executor.setThreadNamePrefix("spider-pool-");
        // setRejectedExecutionHandler：当pool已经达到maxSize的时候，如何处理新进任务
        // CallerRunsPolicy：不在新线程中执行任务，而是由调用者所在的线程来执行
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 等待所有任务结束后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();
        return executor;
    }
    @Bean
    public SocketManager buildSocketManager(Vertx vertx, @Value("${spider.worker.name}") String workerName){
        return new SocketManager(vertx,workerName);
    }
}
