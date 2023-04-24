package cn.spider.framework.spider.log.es.config;

import cn.spider.framework.common.utils.ExceptionMessage;
import cn.spider.framework.spider.log.es.LogVerticle;
import cn.spider.framework.spider.log.es.consumer.LogConsumer;
import cn.spider.framework.spider.log.es.queue.QueueManager;
import cn.spider.framework.spider.log.es.service.SpiderFlowElementExampleService;
import cn.spider.framework.spider.log.es.service.SpiderFlowExampleLogService;
import io.vertx.core.Vertx;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.core.shareddata.SharedData;
import lombok.Data;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

import java.net.InetSocketAddress;

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
    public LogConsumer buildLogConsumer(Vertx vertx,QueueManager queueManager){
        return new LogConsumer(vertx,queueManager);
    }

    @Bean
    public TransportClient buildEsClient(Vertx vertx) {

        SharedData sharedData = vertx.sharedData();
        LocalMap<String, String> localMap = sharedData.getLocalMap("config");

        String esClusterName = localMap.get("es-cluster-name");

        Settings setting = Settings.builder()
                .put("cluster.name", esClusterName)
                .put("client.transport.ignore_cluster_name", true)
                .put("client.transport.sniff", true)
                .build();

        String esAddr = localMap.get("es-ip-addr");

        String esPort = localMap.get("es-port");

        String [] addrs = esAddr.split(",");

        TransportClient client = new PreBuiltTransportClient(setting);
        for(int i = 0;i<addrs.length ;i++){
            TransportAddress transportAddress = new TransportAddress(new InetSocketAddress("192.168.101.21", Integer.parseInt(esPort)));
            client.addTransportAddress(transportAddress);
        }
        return client;
    }

    @Bean("elasticsearchTemplate")
    public ElasticsearchTemplate elasticsearchRestTemplate(TransportClient client) {
        return new ElasticsearchTemplate(client);
    }
}
