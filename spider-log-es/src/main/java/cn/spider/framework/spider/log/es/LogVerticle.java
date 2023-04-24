package cn.spider.framework.spider.log.es;

import cn.spider.framework.spider.log.es.config.LogConfig;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;

public class LogVerticle extends AbstractVerticle {

  public static Vertx clusterVertx;

  private AbstractApplicationContext factory;

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    System.setProperty("es.set.netty.runtime.available.processors","false");
    clusterVertx = vertx;
    factory =  new AnnotationConfigApplicationContext(LogConfig.class);
    startPromise.complete();
  }
}
