package cn.spider.framework.spider.log.es.config;
import cn.spider.framework.spider.log.es.LogVerticle;
import cn.spider.framework.spider.log.es.consumer.LogConsumer;
import cn.spider.framework.spider.log.es.queue.QueueManager;
import cn.spider.framework.spider.log.es.service.SpiderFlowElementExampleService;
import cn.spider.framework.spider.log.es.service.SpiderFlowExampleLogService;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.core.shareddata.SharedData;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

/**
 * @BelongsProject: spider-node
 * @BelongsPackage: cn.spider.framework.spider.log.es.config
 * @Author: dengdongsheng
 * @CreateTime: 2023-04-23  16:40
 * @Description: TODO
 * @Version: 1.0
 */
@ComponentScan("cn.spider.framework.spider.log.es.*")
@EnableElasticsearchRepositories(basePackages = "cn.spider.framework.spider.log.es.dao")
@Configuration
public class LogConfig {

    @Bean
    public QueueManager buildQueueManager(Vertx vertx, SpiderFlowElementExampleService spiderFlowElementExampleService, SpiderFlowExampleLogService exampleLogService){
        return new QueueManager(vertx,spiderFlowElementExampleService,exampleLogService);
    }

    @Bean
    public Vertx buildVertx(){
        return LogVerticle.clusterVertx;
    }

    @Bean
    public LogConsumer buildLogConsumer(EventBus eventBus,QueueManager queueManager,Vertx vertx){
        return new LogConsumer(queueManager,eventBus,vertx);
    }

    @Bean
    public EventBus buildEventBus(Vertx vertx){
        return vertx.eventBus();
    }

    @Bean
    public RestHighLevelClient buildEsClient(Vertx vertx) {
        SharedData sharedData = vertx.sharedData();
        LocalMap<String, String> localMap = sharedData.getLocalMap("config");
        String esAddr = localMap.get("es-ip-addr");
        // 创建Client连接对象
        String[] ips = esAddr.split(",");
        HttpHost[] httpHosts = new HttpHost[ips.length];
        for (int i = 0; i < ips.length; i++) {
            httpHosts[i] = HttpHost.create(ips[i]);
        }
        RestClientBuilder builder = RestClient.builder(httpHosts);
        return new RestHighLevelClient(builder);

    }

    @Bean("elasticsearchTemplate")
    public ElasticsearchRestTemplate elasticsearchRestTemplate(RestHighLevelClient client) {
        return new ElasticsearchRestTemplate(client);
    }

}
