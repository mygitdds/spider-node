package cn.spider.framework.spider.log.es;

import cn.spider.framework.log.sdk.interfaces.LogInterface;
import cn.spider.framework.spider.log.es.config.LogConfig;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceBinder;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;

import java.util.ArrayList;
import java.util.List;

public class LogVerticle extends AbstractVerticle {

  public static Vertx clusterVertx;

  private static AbstractApplicationContext factory;

  private ServiceBinder binder;

  private List<MessageConsumer<JsonObject>> containerConsumers;

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
   // System.setProperty("es.set.netty.runtime.available.processors","false");
    this.clusterVertx = vertx;
    this.factory =  new AnnotationConfigApplicationContext(LogConfig.class);
    LogInterface logInterface = this.factory.getBean(LogInterface.class);

    this.binder = new ServiceBinder(clusterVertx);
    containerConsumers = new ArrayList<>();
    MessageConsumer<JsonObject> containerConsumer = this.binder
            .setAddress(LogInterface.ADDRESS)
            .register(LogInterface.class, logInterface);
    containerConsumers.add(containerConsumer);
    startPromise.complete();
  }

}
