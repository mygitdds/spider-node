package cn.spider.framework.flow.config;

import cn.spider.framework.common.event.EventConfig;
import cn.spider.framework.common.event.EventManager;
import cn.spider.framework.common.utils.BrokerInfoUtil;
import cn.spider.framework.container.sdk.interfaces.BusinessService;
import cn.spider.framework.container.sdk.interfaces.ContainerService;
import cn.spider.framework.db.config.DbRedisConfig;
import cn.spider.framework.db.config.DbRocksConfig;
import cn.spider.framework.db.config.RedisConfig;
import cn.spider.framework.flow.business.BusinessManager;
import cn.spider.framework.flow.container.component.TaskComponentManager;
import cn.spider.framework.flow.container.component.TaskContainer;
import cn.spider.framework.flow.engine.scheduler.SchedulerManager;
import cn.spider.framework.flow.load.loader.ClassLoaderManager;
import cn.spider.framework.flow.load.loader.HotClassLoader;
import cn.spider.framework.flow.SpiderCoreVerticle;
import cn.spider.framework.flow.sync.Publish;
import cn.spider.framework.flow.sync.SyncBusinessRecord;
import cn.spider.framework.flow.timer.SpiderTimer;
import cn.spider.framework.flow.transcript.TranscriptManager;
import cn.spider.framework.linker.sdk.interfaces.LinkerService;
import cn.spider.framework.transaction.sdk.interfaces.TransactionInterface;
import io.vertx.core.Vertx;
import io.vertx.core.WorkerExecutor;
import io.vertx.core.eventbus.EventBus;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @BelongsProject: spider-node
 * @BelongsPackage: cn.spider.framework.flow.config
 * @Author: dengdongsheng
 * @CreateTime: 2023-03-15  13:01
 * @Description: spring-配置类
 * @Version: 1.0
 */
@Configuration
@ComponentScan(basePackages = {"cn.spider.framework.flow.*"})
@Import({RedisConfig.class, DbRocksConfig.class, EventConfig.class})
@Order(-1)
public class SpiderCoreConfig {
    @Bean
    public Vertx buildVertx() {
        return SpiderCoreVerticle.clusterVertx;
    }

    /**
     * 构造vertx的线程池
     *
     * @param vertx
     * @return
     */
    @Bean("businessExecute")
    public WorkerExecutor buildWorkerExecutor(Vertx vertx) {
        int poolSize = 10;
        // 2 minutes
        long maxExecuteTime = 2;
        TimeUnit maxExecuteTimeUnit = TimeUnit.MINUTES;
        WorkerExecutor executor = vertx.createSharedWorkerExecutor("my-worker-pool", poolSize, maxExecuteTime, maxExecuteTimeUnit);
        return executor;
    }

    @Bean
    public ClassLoaderManager buildClassLoaderManager(TaskContainer container, SchedulerManager schedulerManager) {
        ClassLoaderManager classLoaderManager = new ClassLoaderManager();
        classLoaderManager.init((TaskComponentManager) container, schedulerManager);
        return classLoaderManager;
    }

    /**
     * 跟worker通信 接口
     *
     * @param vertx
     * @return
     */
    @Bean
    public LinkerService buildLinkerService(Vertx vertx) {
        return LinkerService.createProxy(vertx, BrokerInfoUtil.queryBrokerName(vertx) + LinkerService.ADDRESS);
    }

    @Bean
    public TransactionInterface buildTransactionInterface(Vertx vertx){
        return TransactionInterface.createProxy(vertx,BrokerInfoUtil.queryBrokerName(vertx) + TransactionInterface.ADDRESS);
    }

    @Bean
    /** 配置成原型（多例），主要是为了更新jar时，使用新的类加载器实例去加载*/
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public HotClassLoader hotClassLoader() {
        return new HotClassLoader(this.getClass().getClassLoader());
    }

    @Bean
    public SchedulerManager buildSchedulerManager(LinkerService linkerService, EventManager eventManager) {
        return new SchedulerManager(linkerService,eventManager);
    }

    @Bean("classLoaderMap")
    public Map<String, ClassLoader> buildClassLoaderMap() {
        return new HashMap<>();
    }

    /**
     * 业务管理器-- 存功能版本信息
     *
     * @param
     * @return
     */
    @Bean
    public BusinessManager buildBusinessManager(RedisTemplate redisTemplate, WorkerExecutor workerExecutor) {
        return new BusinessManager(redisTemplate,workerExecutor);
    }

    /**
     * 把同步数据的对象加入容器
     *
     * @param vertx
     * @param businessService
     * @param containerService
     * @return
     */
    @Bean
    public SyncBusinessRecord buildSyncManager(Vertx vertx, BusinessService businessService, ContainerService containerService) {
        return new SyncBusinessRecord(vertx, businessService, containerService);
    }

    @Bean
    public Publish buildPublish(Vertx vertx) {
        return new Publish(vertx);
    }

    @Bean
    public TranscriptManager buildTranscriptManager(RedisTemplate redisTemplate,
                                                    EventManager eventManager,
                                                    Vertx vertx,
                                                    TransactionInterface transactionInterface,
                                                    SpiderTimer timer){
        return new TranscriptManager(redisTemplate,eventManager,vertx,transactionInterface,timer);
    }

    @Bean
    public EventBus buildEventBus(Vertx vertx){
        return vertx.eventBus();
    }


}
